package com.example.evenue.controller.posts;

import com.example.evenue.models.events.EventModel;
import com.example.evenue.models.posts.PostModel;
import com.example.evenue.models.posts.PostResponse;
import com.example.evenue.models.users.UserModel;
import com.example.evenue.service.EventService;
import com.example.evenue.service.PostService;
import com.example.evenue.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private EventService eventService;

    /**
     * Serve the page for creating a new post.
     */
    @GetMapping("/create")
    public String showCreatePostForm(Model model) {
        List<EventModel> events = eventService.getAllEvents();

        if (events.isEmpty()) {
            System.out.println("No events found!");
        } else {
            System.out.println("Events found: " + events.size());
        }

        model.addAttribute("events", events);
        return "create-post";
    }


    /**
     * Handle the creation of a new post.
     *
     * @param content   The content of the post.
     * @param eventId   The ID of the associated event.
     * @param images    A list of images to upload.
     * @return          A redirect to the posts list page.
     */
    @PostMapping("/create")
    public String createPost(
            @RequestParam String content,
            @RequestParam Long eventId,
            @RequestParam("images") List<MultipartFile> images) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserModel user = userService.findUserByEmail(email);

        postService.createPost(content, eventId, user, images);

        return "redirect:/posts"; // Redirect to the posts list page after creation
    }

    /**
     * Display all posts.
     */
    @GetMapping
    public String getAllPosts(Model model) {
        List<PostResponse> posts = postService.getAllPosts();
        model.addAttribute("posts", posts);
        return "posts-list"; // Return the posts-list.html template
    }

    /**
     * Retrieve a specific post by ID.
     *
     * @param postId The ID of the post to retrieve.
     * @return       A page displaying the post details.
     */
    @GetMapping("/{postId}")
    public String getPostById(@PathVariable Long postId, Model model) {
        // Fetch the post response
        PostResponse post = postService.getPostById(postId);

        // Log the event ID if it's present
        if (post.getEvent() != null) {
            System.out.println("Event ID: " + post.getEvent().getId());
        } else {
            System.out.println("No event associated with this post.");
        }

        // Add the post to the model
        model.addAttribute("post", post);

        return "post-details"; // Return the post-details.html template
    }


    /**
     * Get all posts created by the logged-in user.
     */
    @GetMapping("/my-posts")
    public String getPostsByUser(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserModel user = userService.findUserByEmail(email);

        List<PostResponse> userPosts = postService.getPostsByUserId(Long.valueOf(user.getId()));
        model.addAttribute("posts", userPosts);
        return "my-posts";  // Return the my-posts.html template
    }

    /**
     * Serve the edit post form.
     */
    @GetMapping("/{postId}/edit")
    public String showEditPostForm(@PathVariable Long postId, Model model) {
        PostResponse post = postService.getPostById(postId);
        List<EventModel> events = eventService.getAllEvents();

        model.addAttribute("post", post);
        model.addAttribute("events", events);
        return "edit-post"; // Return the edit-post.html template
    }

    /**
     * Handle the submission of an edited post.
     */
    @PostMapping("/{postId}/edit")
    public String editPost(
            @PathVariable Long postId,
            @RequestParam String title,  // Include the title
            @RequestParam String content,
            @RequestParam Long eventId,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "removeImages", required = false) List<String> removeImages) {

        postService.updatePost(postId, title, content, eventId, images, removeImages); // Pass title as the first parameter
        return "redirect:/posts";
    }




    /**
     * Serve the delete confirmation page.
     */
    @GetMapping("/{postId}/delete")
    public String showDeleteConfirmation(@PathVariable Long postId, Model model) {
        PostResponse post = postService.getPostById(postId);
        model.addAttribute("post", post);
        return "delete-confirmation"; // Return the delete-confirmation.html template
    }

    /**
     * Handle post deletion.
     */
    @PostMapping("/{postId}/delete")
    public String deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return "redirect:/posts";
    }
}
