package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.Blog;

import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    
    @Query("SELECT b FROM Blog b ORDER BY b.datePublished DESC")
    List<Blog> findAllOrderByDatePublishedDesc();
    
    @Query("SELECT b FROM Blog b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY b.datePublished DESC")
    List<Blog> searchByTitle(String searchTerm);
}
