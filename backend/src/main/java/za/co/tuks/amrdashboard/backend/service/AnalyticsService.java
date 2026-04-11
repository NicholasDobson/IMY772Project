package za.co.tuks.amrdashboard.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.tuks.amrdashboard.backend.repository.IsolateRepository;
import za.co.tuks.amrdashboard.backend.repository.WaterSampleRepository;
import za.co.tuks.amrdashboard.backend.repository.SiteRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Month;
import java.time.Year;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    // EntityManager lets us write JPQL queries (like SQL but for Java entities)
    @PersistenceContext
    private EntityManager em;

    private static final Set<String> MDRO_CODES = Set.of("ESBL", "CRE", "MDRO", "MDR", "VRE");

    // ── 1. Sample Count ────────────────────────────────────────────────────────

    public Map<String, Object> getSampleCount() {
        Long total = (Long) em.createQuery(
            "SELECT COUNT(w) FROM WaterSample w"
        ).getSingleResult();

        Long siteCount = (Long) em.createQuery(
            "SELECT COUNT(DISTINCT w.site) FROM WaterSample w"
        ).getSingleResult();

        return Map.of(
            "total", total,
            "siteCount", siteCount
        );
    }

    // ── 2. Incident Rate ───────────────────────────────────────────────────────

    public Map<String, Object> getIncidentRate() {
        Long totalIsolates = (Long) em.createQuery(
            "SELECT COUNT(i) FROM Isolate i"
        ).getSingleResult();

        Long mdroIsolates = (Long) em.createQuery(
            "SELECT COUNT(i) FROM Isolate i WHERE UPPER(i.arCode) IN :codes"
        ).setParameter("codes", MDRO_CODES).getSingleResult();

        double rate = totalIsolates == 0 ? 0.0 :
            Math.round((double) mdroIsolates / totalIsolates * 1000.0) / 10.0;

        int currentYear = Year.now().getValue();
        int lastYear = currentYear - 1;

        // Calculate rate for same period last year
        Long totalLastYear = (Long) em.createQuery(
            "SELECT COUNT(i) FROM Isolate i JOIN i.waterSample w " +
            "WHERE FUNCTION('YEAR', w.collectionDate) = :year"
        ).setParameter("year", lastYear).getSingleResult();

        Long mdroLastYear = (Long) em.createQuery(
            "SELECT COUNT(i) FROM Isolate i JOIN i.waterSample w " +
            "WHERE UPPER(i.arCode) IN :codes " +
            "AND FUNCTION('YEAR', w.collectionDate) = :year"
        ).setParameter("codes", MDRO_CODES).setParameter("year", lastYear).getSingleResult();

        double rateLastYear = totalLastYear == 0 ? 0.0 :
            Math.round((double) mdroLastYear / totalLastYear * 1000.0) / 10.0;

        double delta = Math.round((rate - rateLastYear) * 10.0) / 10.0;
        String direction = delta > 0 ? "up" : delta < 0 ? "down" : "stable";

        return Map.of(
            "rate", rate,
            "comparedToYear", lastYear,
            "delta", Math.abs(delta),
            "direction", direction
        );
    }

    // ── 3. Monthly Cases ───────────────────────────────────────────────────────

    public Map<String, Object> getMonthlyCases(int year, int month) {
        Long caseCount = (Long) em.createQuery(
            "SELECT COUNT(i) FROM Isolate i JOIN i.waterSample w " +
            "WHERE FUNCTION('YEAR', w.collectionDate) = :year " +
            "AND FUNCTION('MONTH', w.collectionDate) = :month"
        ).setParameter("year", year).setParameter("month", month).getSingleResult();

        // Previous month (handle January → December of previous year)
        int prevMonth = month == 1 ? 12 : month - 1;
        int prevYear = month == 1 ? year - 1 : year;

        Long previousMonthCount = (Long) em.createQuery(
            "SELECT COUNT(i) FROM Isolate i JOIN i.waterSample w " +
            "WHERE FUNCTION('YEAR', w.collectionDate) = :year " +
            "AND FUNCTION('MONTH', w.collectionDate) = :month"
        ).setParameter("year", prevYear).setParameter("month", prevMonth).getSingleResult();

        long delta = caseCount - previousMonthCount;
        String direction = delta > 0 ? "up" : delta < 0 ? "down" : "stable";

        return Map.of(
            "year", year,
            "month", month,
            "caseCount", caseCount,
            "previousMonthCount", previousMonthCount,
            "delta", delta,
            "direction", direction
        );
    }

    // ── 4. Monthly Trend ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMonthlyTrend(int year) {
        // Returns list of [monthNumber, count]
        List<Object[]> rows = em.createQuery(
            "SELECT FUNCTION('MONTH', w.collectionDate), COUNT(i) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "WHERE FUNCTION('YEAR', w.collectionDate) = :year " +
            "GROUP BY FUNCTION('MONTH', w.collectionDate) " +
            "ORDER BY FUNCTION('MONTH', w.collectionDate)"
        ).setParameter("year", year).getResultList();

        // Map results into month → count lookup
        Map<Integer, Long> countByMonth = new HashMap<>();
        for (Object[] row : rows) {
            countByMonth.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }

        // Build rolling 6-month average for alert detection
        String[] monthLabels = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        List<Map<String, Object>> data = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            long count = countByMonth.getOrDefault(m, 0L);

            // Rolling 6-month average: average of the 6 months before this one
            double rollingAvg = 0;
            int windowMonths = 0;
            for (int w = m - 6; w < m; w++) {
                int windowMonth = w <= 0 ? w + 12 : w;
                rollingAvg += countByMonth.getOrDefault(windowMonth, 0L);
                windowMonths++;
            }
            if (windowMonths > 0) rollingAvg /= windowMonths;

            // Alert if count is more than 20% above the rolling average
            boolean alert = rollingAvg > 0 && count > rollingAvg * 1.2;

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", m);
            point.put("monthLabel", monthLabels[m - 1]);
            point.put("caseCount", count);
            point.put("alert", alert);
            data.add(point);
        }

        return Map.of("year", year, "data", data);
    }

    // ── 5. Top Organisms ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTopOrganisms(int limit) {
        int currentYear = Year.now().getValue();

        // Get current year counts per organism
        List<Object[]> currentRows = em.createQuery(
            "SELECT i.organismIdentity, i.arCode, COUNT(i), COUNT(DISTINCT w.site) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "WHERE FUNCTION('YEAR', w.collectionDate) = :year " +
            "GROUP BY i.organismIdentity, i.arCode " +
            "ORDER BY COUNT(i) DESC"
        ).setParameter("year", currentYear).setMaxResults(limit).getResultList();

        // Get previous year counts for YoY trend
        List<Object[]> prevRows = em.createQuery(
            "SELECT i.organismIdentity, COUNT(i) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "WHERE FUNCTION('YEAR', w.collectionDate) = :year " +
            "GROUP BY i.organismIdentity"
        ).setParameter("year", currentYear - 1).getResultList();

        Map<String, Long> prevYearCounts = new HashMap<>();
        for (Object[] row : prevRows) {
            prevYearCounts.put((String) row[0], ((Number) row[1]).longValue());
        }

        List<Map<String, Object>> organisms = new ArrayList<>();
        for (Object[] row : currentRows) {
            String name = (String) row[0];
            String arCode = (String) row[1];
            long detectionCount = ((Number) row[2]).longValue();
            long siteCount = ((Number) row[3]).longValue();

            long prevCount = prevYearCounts.getOrDefault(name, 0L);
            String yoyTrend;
            if (prevCount == 0) {
                yoyTrend = "stable";
            } else {
                double changePct = (double)(detectionCount - prevCount) / prevCount * 100;
                yoyTrend = changePct > 5 ? "up" : changePct < -5 ? "down" : "stable";
            }

            Map<String, Object> organism = new LinkedHashMap<>();
            organism.put("name", name);
            organism.put("arCode", arCode);
            organism.put("detectionCount", detectionCount);
            organism.put("siteCount", siteCount);
            organism.put("yoyTrend", yoyTrend);
            organisms.add(organism);
        }

        return Map.of("organisms", organisms);
    }
}
