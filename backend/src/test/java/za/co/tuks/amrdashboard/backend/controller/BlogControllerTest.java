package za.co.tuks.amrdashboard.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.tuks.amrdashboard.backend.model.Blog;
import za.co.tuks.amrdashboard.backend.service.BlogService;
import za.co.tuks.amrdashboard.backend.service.FileStorageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BlogController.class)
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private BlogService blogService;

    @MockitoBean
    private FileStorageService fileStorageService;

    private Blog buildBlog(Long id, String title) {
        Blog blog = new Blog();
        blog.setBlogId(id);
        blog.setTitle(title);
        blog.setAuthor("Test Author");
        blog.setContent("Test content body");
        blog.setDatePublished(LocalDateTime.of(2025, 5, 10, 9, 0));
        return blog;
    }

    @Test
    void shouldReturnAllBlogs() throws Exception {
        when(blogService.getAllBlogs()).thenReturn(List.of(
                buildBlog(1L, "AMR Trends 2025"),
                buildBlog(2L, "Water Quality Report")
        ));

        mockMvc.perform(get("/api/v1/blogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].blogId").value(1))
                .andExpect(jsonPath("$[0].title").value("AMR Trends 2025"))
                .andExpect(jsonPath("$[1].title").value("Water Quality Report"));
    }

    @Test
    void shouldReturnEmptyListWhenNoBlogs() throws Exception {
        when(blogService.getAllBlogs()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/blogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnBlogById() throws Exception {
        when(blogService.getBlogById(1L)).thenReturn(Optional.of(buildBlog(1L, "AMR Trends 2025")));

        mockMvc.perform(get("/api/v1/blogs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blogId").value(1))
                .andExpect(jsonPath("$.title").value("AMR Trends 2025"))
                .andExpect(jsonPath("$.author").value("Test Author"));
    }

    @Test
    void shouldReturn404WhenBlogNotFound() throws Exception {
        when(blogService.getBlogById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/blogs/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateBlogFromJson() throws Exception {
        Blog input = buildBlog(null, "New Research Post");
        Blog saved = buildBlog(3L, "New Research Post");
        when(blogService.createBlog(any(Blog.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blogId").value(3))
                .andExpect(jsonPath("$.title").value("New Research Post"));
    }

    @Test
    void shouldUpdateExistingBlog() throws Exception {
        Blog updated = buildBlog(1L, "Updated Title");
        when(blogService.updateBlog(eq(1L), any(Blog.class))).thenReturn(Optional.of(updated));

        mockMvc.perform(put("/api/v1/blogs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentBlog() throws Exception {
        when(blogService.updateBlog(eq(99L), any(Blog.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/blogs/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBlog(null, "Anything"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteExistingBlog() throws Exception {
        when(blogService.deleteBlog(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/blogs/1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentBlog() throws Exception {
        when(blogService.deleteBlog(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/blogs/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSearchBlogsByTitle() throws Exception {
        when(blogService.searchBlogs("AMR")).thenReturn(List.of(
                buildBlog(1L, "AMR Trends 2025")
        ));

        mockMvc.perform(get("/api/v1/blogs/search").param("query", "AMR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("AMR Trends 2025"));
    }

    @Test
    void shouldReturnEmptyListWhenSearchFindsNothing() throws Exception {
        when(blogService.searchBlogs("xyz")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/blogs/search").param("query", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
