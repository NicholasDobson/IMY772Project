package za.co.tuks.amrdashboard.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.tuks.amrdashboard.backend.model.Blog;
import za.co.tuks.amrdashboard.backend.service.BlogService;
import za.co.tuks.amrdashboard.backend.service.FileStorageService;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final FileStorageService fileStorageService;

    /**
     * Get all blogs ordered by date published (newest first)
     */
    @GetMapping
    public ResponseEntity<List<Blog>> getAllBlogs() {
        List<Blog> blogs = blogService.getAllBlogs();
        return ResponseEntity.ok(blogs);
    }

    /**
     * Get a specific blog by ID
     */
    @GetMapping("/{blogId}")
    public ResponseEntity<Object> getBlogById(@PathVariable Long blogId) {
        Optional<Blog> blog = blogService.getBlogById(blogId);
        if (blog.isPresent()) {
            return ResponseEntity.ok(blog.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Blog not found with ID: " + blogId);
        }
    }

    /**
     * Create a new blog post from JSON
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createBlog(@RequestBody Blog blog) {
        try {
            Blog createdBlog = blogService.createBlog(blog);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBlog);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating blog: " + e.getMessage());
        }
    }

    /**
     * Create a new blog post with a local image upload
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> createBlog(
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String content,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            Blog blog = new Blog();
            blog.setTitle(title);
            blog.setAuthor(author);
            blog.setContent(content);
            if (image != null && !image.isEmpty()) {
                String imagePath = fileStorageService.storeFile(image);
                blog.setImage(imagePath);
            }
            Blog createdBlog = blogService.createBlog(blog);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBlog);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating blog: " + e.getMessage());
        }
    }

    /**
     * Update an existing blog post
     */
    @PutMapping("/{blogId}")
    public ResponseEntity<Object> updateBlog(@PathVariable Long blogId, @RequestBody Blog blogDetails) {
        Optional<Blog> blog = blogService.updateBlog(blogId, blogDetails);
        if (blog.isPresent()) {
            return ResponseEntity.ok(blog.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Blog not found with ID: " + blogId);
        }
    }

    /**
     * Delete a blog post
     */
    @DeleteMapping("/{blogId}")
    public ResponseEntity<Object> deleteBlog(@PathVariable Long blogId) {
        boolean deleted = blogService.deleteBlog(blogId);
        if (deleted) {
            return ResponseEntity.ok("Blog deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Blog not found with ID: " + blogId);
        }
    }

    /**
     * Search blogs by title
     */
    @GetMapping("/search")
    public ResponseEntity<List<Blog>> searchBlogs(@RequestParam String query) {
        List<Blog> blogs = blogService.searchBlogs(query);
        return ResponseEntity.ok(blogs);
    }
}
