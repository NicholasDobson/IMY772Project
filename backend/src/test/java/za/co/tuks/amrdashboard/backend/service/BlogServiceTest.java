package za.co.tuks.amrdashboard.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.tuks.amrdashboard.backend.model.Blog;
import za.co.tuks.amrdashboard.backend.repository.BlogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogRepository blogRepository;

    @InjectMocks
    private BlogService blogService;

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
    void shouldReturnAllBlogsOrderedByDate() {
        List<Blog> expected = List.of(
                buildBlog(2L, "Newer Post"),
                buildBlog(1L, "Older Post")
        );
        when(blogRepository.findAllOrderByDatePublishedDesc()).thenReturn(expected);

        List<Blog> result = blogService.getAllBlogs();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Newer Post");
        assertThat(result.get(1).getTitle()).isEqualTo("Older Post");
    }

    @Test
    void shouldReturnBlogById() {
        Blog blog = buildBlog(1L, "AMR Trends 2025");
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));

        Optional<Blog> result = blogService.getBlogById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("AMR Trends 2025");
    }

    @Test
    void shouldReturnEmptyWhenBlogNotFound() {
        when(blogRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Blog> result = blogService.getBlogById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldCreateBlogAndSetTimestampWhenNotProvided() {
        Blog input = new Blog();
        input.setTitle("New Post");
        input.setAuthor("Author");
        input.setContent("Content");

        Blog saved = buildBlog(5L, "New Post");
        when(blogRepository.save(any(Blog.class))).thenReturn(saved);

        Blog result = blogService.createBlog(input);

        assertThat(input.getDatePublished()).isNotNull();
        assertThat(result.getBlogId()).isEqualTo(5L);
        verify(blogRepository).save(input);
    }

    @Test
    void shouldNotOverwriteTimestampWhenAlreadyProvided() {
        LocalDateTime existingDate = LocalDateTime.of(2024, 1, 15, 12, 0);
        Blog input = new Blog();
        input.setTitle("Old Post");
        input.setAuthor("Author");
        input.setContent("Content");
        input.setDatePublished(existingDate);

        when(blogRepository.save(any(Blog.class))).thenReturn(input);

        blogService.createBlog(input);

        assertThat(input.getDatePublished()).isEqualTo(existingDate);
    }

    @Test
    void shouldUpdateExistingBlogFields() {
        Blog existing = buildBlog(1L, "Original Title");
        Blog updates = new Blog();
        updates.setTitle("Updated Title");
        updates.setContent("Updated content");

        when(blogRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(blogRepository.save(existing)).thenReturn(existing);

        Optional<Blog> result = blogService.updateBlog(1L, updates);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Updated Title");
        assertThat(result.get().getContent()).isEqualTo("Updated content");
        assertThat(result.get().getAuthor()).isEqualTo("Test Author");
    }

    @Test
    void shouldNotUpdateFieldsWhenUpdateValuesAreNull() {
        Blog existing = buildBlog(1L, "Original Title");
        Blog updates = new Blog();

        when(blogRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(blogRepository.save(existing)).thenReturn(existing);

        Optional<Blog> result = blogService.updateBlog(1L, updates);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Original Title");
        assertThat(result.get().getAuthor()).isEqualTo("Test Author");
    }

    @Test
    void shouldReturnEmptyWhenUpdatingNonExistentBlog() {
        when(blogRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Blog> result = blogService.updateBlog(99L, buildBlog(null, "Anything"));

        assertThat(result).isEmpty();
        verify(blogRepository, never()).save(any());
    }

    @Test
    void shouldDeleteExistingBlogAndReturnTrue() {
        when(blogRepository.existsById(1L)).thenReturn(true);

        boolean deleted = blogService.deleteBlog(1L);

        assertThat(deleted).isTrue();
        verify(blogRepository).deleteById(1L);
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentBlog() {
        when(blogRepository.existsById(99L)).thenReturn(false);

        boolean deleted = blogService.deleteBlog(99L);

        assertThat(deleted).isFalse();
        verify(blogRepository, never()).deleteById(any());
    }

    @Test
    void shouldSearchBlogsByTitle() {
        when(blogRepository.searchByTitle("AMR")).thenReturn(List.of(
                buildBlog(1L, "AMR Trends 2025")
        ));

        List<Blog> result = blogService.searchBlogs("AMR");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("AMR Trends 2025");
    }

    @Test
    void shouldReturnEmptyListWhenSearchFindsNothing() {
        when(blogRepository.searchByTitle("xyz")).thenReturn(List.of());

        List<Blog> result = blogService.searchBlogs("xyz");

        assertThat(result).isEmpty();
    }
}
