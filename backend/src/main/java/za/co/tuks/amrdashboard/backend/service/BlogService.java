package za.co.tuks.amrdashboard.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.tuks.amrdashboard.backend.model.Blog;
import za.co.tuks.amrdashboard.backend.repository.BlogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;

    /**
     * Get all blogs ordered by date published (newest first)
     */
    public List<Blog> getAllBlogs() {
        return blogRepository.findAllOrderByDatePublishedDesc();
    }

    /**
     * Get a blog by its ID
     */
    public Optional<Blog> getBlogById(Long blogId) {
        return blogRepository.findById(blogId);
    }

    /**
     * Create a new blog post
     */
    public Blog createBlog(Blog blog) {
        if (blog.getDatePublished() == null) {
            blog.setDatePublished(LocalDateTime.now());
        }
        return blogRepository.save(blog);
    }

    /**
     * Update an existing blog post
     */
    public Optional<Blog> updateBlog(Long blogId, Blog blogDetails) {
        return blogRepository.findById(blogId).map(blog -> {
            if (blogDetails.getTitle() != null) {
                blog.setTitle(blogDetails.getTitle());
            }
            if (blogDetails.getImage() != null) {
                blog.setImage(blogDetails.getImage());
            }
            if (blogDetails.getAuthor() != null) {
                blog.setAuthor(blogDetails.getAuthor());
            }
            if (blogDetails.getContent() != null) {
                blog.setContent(blogDetails.getContent());
            }
            return blogRepository.save(blog);
        });
    }

    /**
     * Delete a blog post by its ID
     */
    public boolean deleteBlog(Long blogId) {
        if (blogRepository.existsById(blogId)) {
            blogRepository.deleteById(blogId);
            return true;
        }
        return false;
    }

    /**
     * Search blogs by title
     */
    public List<Blog> searchBlogs(String searchTerm) {
        return blogRepository.searchByTitle(searchTerm);
    }
}
