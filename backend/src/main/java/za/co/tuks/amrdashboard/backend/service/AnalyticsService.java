package za.co.tuks.amrdashboard.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.tuks.amrdashboard.backend.repository.IsolateRepository;
import za.co.tuks.amrdashboard.backend.repository.WaterSampleRepository;
import za.co.tuks.amrdashboard.backend.repository.SiteRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
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

    // Static river-name → province mapping (no province column on Site)
    private static final Map<String, String> RIVER_PROVINCE;
    static {
        RIVER_PROVINCE = new LinkedHashMap<>();
        RIVER_PROVINCE.put("Apies River",      "Gauteng");
        RIVER_PROVINCE.put("Hennops River",     "Gauteng");
        RIVER_PROVINCE.put("Crocodile River",   "Gauteng");
        RIVER_PROVINCE.put("Jukskei River",     "Gauteng");
        RIVER_PROVINCE.put("Blesbokspruit",     "Gauteng");
        RIVER_PROVINCE.put("Vaal River",        "Free State");
        RIVER_PROVINCE.put("Tugela River",      "KwaZulu-Natal");
        RIVER_PROVINCE.put("Umgeni River",      "KwaZulu-Natal");
        RIVER_PROVINCE.put("Breede River",      "Western Cape");
        RIVER_PROVINCE.put("Berg River",        "Western Cape");
        RIVER_PROVINCE.put("Fish River",        "Eastern Cape");
        RIVER_PROVINCE.put("Sundays River",     "Eastern Cape");
        RIVER_PROVINCE.put("Limpopo River",     "Limpopo");
        RIVER_PROVINCE.put("Olifants River",    "Limpopo");
        RIVER_PROVINCE.put("Komati River",      "Mpumalanga");
        RIVER_PROVINCE.put("Sabie River",       "Mpumalanga");
        RIVER_PROVINCE.put("Molopo River",      "North West");
        RIVER_PROVINCE.put("Harts River",       "North West");
    }

    private String provinceForRiver(String riverName) {
        if (riverName == null) return "Unknown";
        for (Map.Entry<String, String> e : RIVER_PROVINCE.entrySet()) {
            if (riverName.toLowerCase().contains(e.getKey().toLowerCase())) return e.getValue();
        }
        return "Unknown";
    }

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
            "WHERE EXTRACT(YEAR FROM w.collectionDate) = :year"
        ).setParameter("year", lastYear).getSingleResult();

        Long mdroLastYear = (Long) em.createQuery(
            "SELECT COUNT(i) FROM Isolate i JOIN i.waterSample w " +
            "WHERE UPPER(i.arCode) IN :codes " +
            "AND EXTRACT(YEAR FROM w.collectionDate) = :year"
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
            "WHERE EXTRACT(YEAR FROM w.collectionDate) = :year " +
            "AND EXTRACT(MONTH FROM w.collectionDate) = :month"
        ).setParameter("year", year).setParameter("month", month).getSingleResult();

        // Previous month (handle January → December of previous year)
        int prevMonth = month == 1 ? 12 : month - 1;
        int prevYear = month == 1 ? year - 1 : year;

        Long previousMonthCount = (Long) em.createQuery(
            "SELECT COUNT(i) FROM Isolate i JOIN i.waterSample w " +
            "WHERE EXTRACT(YEAR FROM w.collectionDate) = :year " +
            "AND EXTRACT(MONTH FROM w.collectionDate) = :month"
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
        // Current year counts
        List<Object[]> rows = em.createQuery(
            "SELECT EXTRACT(MONTH FROM w.collectionDate), COUNT(i) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "WHERE EXTRACT(YEAR FROM w.collectionDate) = :year " +
            "GROUP BY EXTRACT(MONTH FROM w.collectionDate) " +
            "ORDER BY EXTRACT(MONTH FROM w.collectionDate)"
        ).setParameter("year", year).getResultList();

        Map<Integer, Long> countByMonth = new HashMap<>();
        for (Object[] row : rows) {
            countByMonth.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }

        // Previous year counts — used to fill the rolling window for Jan–Jun
        // so the baseline does not artificially reset at the year boundary.
        List<Object[]> prevRows = em.createQuery(
            "SELECT EXTRACT(MONTH FROM w.collectionDate), COUNT(i) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "WHERE EXTRACT(YEAR FROM w.collectionDate) = :prevYear " +
            "GROUP BY EXTRACT(MONTH FROM w.collectionDate)"
        ).setParameter("prevYear", year - 1).getResultList();

        Map<Integer, Long> prevYearByMonth = new HashMap<>();
        for (Object[] row : prevRows) {
            prevYearByMonth.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }

        String[] monthLabels = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        List<Map<String, Object>> data = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            long count = countByMonth.getOrDefault(m, 0L);

            // Rolling 6-month average crossing the year boundary:
            // months w ≤ 0 are fetched from the previous year's map.
            double rollingAvg = 0;
            int windowMonths = 0;
            for (int w = m - 6; w < m; w++) {
                long windowCount;
                if (w <= 0) {
                    windowCount = prevYearByMonth.getOrDefault(w + 12, 0L);
                } else {
                    windowCount = countByMonth.getOrDefault(w, 0L);
                }
                rollingAvg += windowCount;
                windowMonths++;
            }
            if (windowMonths > 0) rollingAvg /= windowMonths;

            // Elevated: current month > 20 % above rolling average
            // Require the average to be non-zero (i.e. there IS a prior baseline).
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

    // ── 4a. Available years ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAvailableYears() {
        List<Number> rows = em.createQuery(
            "SELECT DISTINCT EXTRACT(YEAR FROM w.collectionDate) " +
            "FROM WaterSample w " +
            "ORDER BY EXTRACT(YEAR FROM w.collectionDate) DESC"
        ).getResultList();

        List<Integer> years = rows.stream()
            .map(Number::intValue)
            .collect(java.util.stream.Collectors.toList());

        return Map.of("years", years);
    }

    // ── 4b. Monthly Trend — best year (most months with data) ─────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMonthlyTrendBestYear() {
        // Find the year with the most distinct months containing isolates
        List<Object[]> yearRows = em.createQuery(
            "SELECT EXTRACT(YEAR FROM w.collectionDate), COUNT(DISTINCT EXTRACT(MONTH FROM w.collectionDate)) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "GROUP BY EXTRACT(YEAR FROM w.collectionDate) " +
            "ORDER BY COUNT(DISTINCT EXTRACT(MONTH FROM w.collectionDate)) DESC, " +
            "EXTRACT(YEAR FROM w.collectionDate) DESC"
        ).setMaxResults(1).getResultList();

        int bestYear = yearRows.isEmpty()
            ? Year.now().getValue()
            : ((Number) yearRows.get(0)[0]).intValue();

        return getMonthlyTrend(bestYear);
    }

    // ── 5. Top Organisms ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTopOrganisms(int limit) {
        int currentYear = Year.now().getValue();

        List<Object[]> currentRows = em.createQuery(
            "SELECT i.organismIdentity, i.arCode, COUNT(i), COUNT(DISTINCT w.site) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "WHERE EXTRACT(YEAR FROM w.collectionDate) = :year " +
            "GROUP BY i.organismIdentity, i.arCode " +
            "ORDER BY COUNT(i) DESC"
        ).setParameter("year", currentYear).setMaxResults(limit).getResultList();

        // Previous year counts for YoY trend
        List<Object[]> prevRows = em.createQuery(
            "SELECT i.organismIdentity, COUNT(i) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "WHERE EXTRACT(YEAR FROM w.collectionDate) = :year " +
            "GROUP BY i.organismIdentity"
        ).setParameter("year", currentYear - 1).getResultList();

        Map<String, Long> prevYearCounts = new HashMap<>();
        for (Object[] row : prevRows) prevYearCounts.put((String) row[0], ((Number) row[1]).longValue());

        // MDRO counts per organism for resistance rate
        List<Object[]> mdroRows = em.createQuery(
            "SELECT i.organismIdentity, COUNT(i) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "WHERE EXTRACT(YEAR FROM w.collectionDate) = :year " +
            "AND UPPER(i.arCode) IN :codes " +
            "GROUP BY i.organismIdentity"
        ).setParameter("year", currentYear).setParameter("codes", MDRO_CODES).getResultList();

        Map<String, Long> mdroCounts = new HashMap<>();
        for (Object[] row : mdroRows) mdroCounts.put((String) row[0], ((Number) row[1]).longValue());

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

            long mdro = mdroCounts.getOrDefault(name, 0L);
            double resistanceRate = detectionCount == 0 ? 0.0
                : Math.round((double) mdro / detectionCount * 1000.0) / 10.0;

            Map<String, Object> organism = new LinkedHashMap<>();
            organism.put("name", name);
            organism.put("arCode", arCode);
            organism.put("detectionCount", detectionCount);
            organism.put("siteCount", siteCount);
            organism.put("yoyTrend", yoyTrend);
            organism.put("resistanceRate", resistanceRate);
            organisms.add(organism);
        }

        return Map.of("organisms", organisms);
    }

    // ── 6. High-Risk Sites ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getHighRiskSites() {
        // A site is high-risk if ≥1 MDRO isolate has been collected there
        List<String> highRiskSiteIds = em.createQuery(
            "SELECT DISTINCT w.site.siteId FROM Isolate i JOIN i.waterSample w " +
            "WHERE UPPER(i.arCode) IN :codes",
            String.class
        ).setParameter("codes", MDRO_CODES).getResultList();

        int count = highRiskSiteIds.size();

        // "new this month" = site's first MDRO detection falls in the current month
        LocalDate now = LocalDate.now();
        int year  = now.getYear();
        int month = now.getMonthValue();

        List<Object[]> firstDetections = em.createQuery(
            "SELECT w.site.siteId, MIN(w.collectionDate) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "WHERE UPPER(i.arCode) IN :codes " +
            "GROUP BY w.site.siteId"
        ).setParameter("codes", MDRO_CODES).getResultList();

        long newThisMonth = firstDetections.stream()
            .filter(row -> {
                LocalDate d = (LocalDate) row[1];
                return d != null && d.getYear() == year && d.getMonthValue() == month;
            }).count();

        return Map.of("count", count, "newThisMonth", newThisMonth);
    }

    // ── 7. Risk by Province ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRiskByProvince() {
        // Aggregate isolate and MDRO counts per river, then roll up to province
        List<Object[]> riverStats = em.createQuery(
            "SELECT w.site.riverName, COUNT(i), " +
            "SUM(CASE WHEN UPPER(i.arCode) IN :codes THEN 1 ELSE 0 END) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "GROUP BY w.site.riverName"
        ).setParameter("codes", MDRO_CODES).getResultList();

        // Accumulate totals per province
        Map<String, long[]> byProvince = new LinkedHashMap<>(); // province → [total, mdro]
        for (Object[] row : riverStats) {
            String river = (String) row[0];
            long total = ((Number) row[1]).longValue();
            long mdro  = ((Number) row[2]).longValue();
            String province = provinceForRiver(river);
            if ("Unknown".equals(province)) continue;
            byProvince.computeIfAbsent(province, k -> new long[]{0L, 0L});
            byProvince.get(province)[0] += total;
            byProvince.get(province)[1] += mdro;
        }

        if (byProvince.isEmpty()) {
            return Map.of("provinces", List.of());
        }

        // Determine max MDRO count for normalising the 0-100 riskScore bar
        long maxMdro = byProvince.values().stream()
            .mapToLong(a -> a[1]).max().orElse(1L);

        List<Map<String, Object>> provinces = new ArrayList<>();
        byProvince.forEach((name, counts) -> {
            long total = counts[0];
            long mdro  = counts[1];
            double rate = total == 0 ? 0 : (double) mdro / total;
            String riskLevel = rate >= 0.5 ? "HIGH" : rate >= 0.2 ? "MED" : "LOW";
            int riskScore = maxMdro == 0 ? 0 : (int) Math.round((double) mdro / maxMdro * 100);

            Map<String, Object> p = new LinkedHashMap<>();
            p.put("name", name);
            p.put("riskLevel", riskLevel);
            p.put("riskScore", Math.max(riskScore, 8)); // min bar width for visibility
            p.put("mdroCount", mdro);
            provinces.add(p);
        });

        // Sort HIGH → MED → LOW, then by mdroCount desc
        provinces.sort((a, b) -> {
            int ra = riskOrder((String) a.get("riskLevel"));
            int rb = riskOrder((String) b.get("riskLevel"));
            if (ra != rb) return Integer.compare(ra, rb);
            return Long.compare(((Number) b.get("mdroCount")).longValue(),
                                ((Number) a.get("mdroCount")).longValue());
        });

        return Map.of("provinces", provinces);
    }

    private int riskOrder(String level) {
        return switch (level) { case "HIGH" -> 0; case "MED" -> 1; default -> 2; };
    }

    // ── 8. Top Resistance Genes ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTopResistanceGenes(int limit) {
        List<Object[]> rows = em.createQuery(
            "SELECT a.geneSymbol, a.resistanceClass, a.resistanceSubclass, " +
            "COUNT(DISTINCT a.isolate.isolateId), AVG(a.identityPercentage) " +
            "FROM AmrSequence a " +
            "WHERE a.geneSymbol IS NOT NULL " +
            "GROUP BY a.geneSymbol, a.resistanceClass, a.resistanceSubclass " +
            "ORDER BY COUNT(DISTINCT a.isolate.isolateId) DESC"
        ).setMaxResults(limit).getResultList();

        List<Map<String, Object>> genes = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> gene = new LinkedHashMap<>();
            gene.put("gene",            row[0]);
            gene.put("resistanceClass", row[1] != null ? row[1] : "OTHER");
            gene.put("subclass",        row[2] != null ? row[2] : "");
            gene.put("isolates",        ((Number) row[3]).longValue());
            double avgId = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            gene.put("identity", Math.round(avgId * 10.0) / 10.0);
            genes.add(gene);
        }

        return Map.of("genes", genes);
    }

    // ── 9. Affected River Sites ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAffectedRiverSites(int limit) {
        List<Object[]> rows = em.createQuery(
            "SELECT w.site.siteId, w.site.riverName, MAX(w.collectionDate), " +
            "COUNT(DISTINCT i.isolateId), " +
            "SUM(CASE WHEN UPPER(i.arCode) IN :codes THEN 1 ELSE 0 END) " +
            "FROM Isolate i JOIN i.waterSample w " +
            "GROUP BY w.site.siteId, w.site.riverName " +
            "ORDER BY COUNT(DISTINCT i.isolateId) DESC"
        ).setParameter("codes", MDRO_CODES).setMaxResults(limit).getResultList();

        List<Map<String, Object>> sites = new ArrayList<>();
        for (Object[] row : rows) {
            String siteId   = (String) row[0];
            String river    = (String) row[1];
            LocalDate lastDate = (LocalDate) row[2];
            long isolateCount  = ((Number) row[3]).longValue();
            long mdroCount     = ((Number) row[4]).longValue();
            double rate = isolateCount == 0 ? 0 : (double) mdroCount / isolateCount;
            String riskLevel = rate >= 0.5 ? "HIGH" : rate >= 0.2 ? "MED" : "LOW";

            Map<String, Object> site = new LinkedHashMap<>();
            site.put("siteId",      siteId);
            site.put("river",       river);
            site.put("province",    provinceForRiver(river));
            site.put("lastSampled", lastDate != null ? lastDate.toString() : "");
            site.put("isolates",    isolateCount);
            site.put("risk",        riskLevel);
            sites.add(site);
        }

        return Map.of("sites", sites);
    }
}
