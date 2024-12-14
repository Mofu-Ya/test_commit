package com.example.samuraitravel.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.Review;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.form.ReviewEditForm;
import com.example.samuraitravel.form.ReviewRegisterForm;
import com.example.samuraitravel.repository.HouseRepository;
import com.example.samuraitravel.repository.ReviewRepository;
import com.example.samuraitravel.repository.UserRepository;
import com.example.samuraitravel.security.UserDetailsImpl;
import com.example.samuraitravel.service.HouseService;
import com.example.samuraitravel.service.ReviewService;

@Controller
@RequestMapping("/houses/{id}/reviews")
public class ReviewController {
	private final ReviewService reviewService;
	private final HouseService houseService;
	private final HouseRepository houseRepository;
	private final ReviewRepository reviewRepository;
	private final UserRepository userRepository;

	public ReviewController(ReviewService reviewService, HouseService houseService, HouseRepository houseRepository, ReviewRepository reviewRepository, UserRepository userRepository) {
		this.reviewService = reviewService;
		this.houseService = houseService;
		this.houseRepository = houseRepository;
		this.reviewRepository = reviewRepository;
		this.userRepository = userRepository;
	}

	// レビュー一覧を表示
	@GetMapping
	public String list(@PathVariable(name = "id") Integer houseId, @AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @PageableDefault(page = 0, size = 10, sort = "id", direction = Direction.ASC) Pageable pageable, Model model) {
		House house = houseRepository.getReferenceById(houseId);
		Page<Review> reviews = reviewRepository.findByHouseOrderByCreatedAtDesc(house, pageable);
		User user;
		boolean hasReviewed;
		if(userDetailsImpl != null) {
			// ログイン済ユーザーの場合
			user = userRepository.getReferenceById(userDetailsImpl.getUser().getId());
			model.addAttribute("currentUser", user);
			hasReviewed = reviewService.hasUserReviewedHouse(house, user);
		} else {
			// 未ログインユーザーの場合
			model.addAttribute("currentUser", "");
			hasReviewed = false;
		}
		
		Page<Review> reviewPage;
		reviewPage = reviewRepository.findAll(pageable);
		
		model.addAttribute("house", house);
		model.addAttribute("reviews", reviews);
		model.addAttribute("hasReviewed", hasReviewed);
		model.addAttribute("reviewPage", reviewPage); 
		return "reviews/list";
	}
	

	@GetMapping("/register")
	public String register(@PathVariable(name = "id") Integer houseId, Model model) {
		House house = houseRepository.getReferenceById(houseId);
		model.addAttribute("reviewRegisterForm", new ReviewRegisterForm());
		model.addAttribute("house", house);
		return "reviews/register";
		
	}
	
	@PostMapping("/create")
    public String create(@PathVariable(name = "id") Integer houseId, @AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @ModelAttribute @Validated ReviewRegisterForm reviewRegisterForm, BindingResult bindingResult, RedirectAttributes redirectAttributes) {        
		House house = houseRepository.getReferenceById(houseId);
		User user = userDetailsImpl.getUser();
		if (bindingResult.hasErrors()) {
            return "reviews/register";
        }

        reviewService.create(house, user, reviewRegisterForm);
        redirectAttributes.addFlashAttribute("successMessage", "レビューを投稿しました。");    
        
        return "redirect:/houses/" + houseId;
    } 
	
	@GetMapping("/{review_id}/edit")
	public String edit(@PathVariable(name = "id") Integer houseId, @PathVariable(name = "review_id") Integer reviewId, Model model) {
		House house = houseRepository.getReferenceById(houseId);
		Review review = reviewRepository.getReferenceById(reviewId);
		ReviewEditForm reviewEditForm = new ReviewEditForm(review.getId(), review.getScore(), review.getImpression());
		
		model.addAttribute("reviewEditForm", reviewEditForm);
		model.addAttribute("house", house);
		model.addAttribute("review", review);
		return "reviews/edit";
		
	}
	
	@PostMapping("/{review_id}/update")
    public String update(@PathVariable(name = "id") Integer houseId, @PathVariable(name = "review_id") Integer reviewId, @AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @ModelAttribute @Validated ReviewEditForm reviewEditForm, BindingResult bindingResult, RedirectAttributes redirectAttributes) {        
		House house = houseRepository.getReferenceById(houseId);
		User user = userDetailsImpl.getUser();
		if (bindingResult.hasErrors()) {
            return "reviews/edit";
        }

        reviewService.update(house, user, reviewEditForm);
        redirectAttributes.addFlashAttribute("successMessage", "レビューを更新しました。");    
        
        return "redirect:/houses/" + houseId;
    } 
	
	@PostMapping("/{review_id}/delete")
	public String delete(@PathVariable(name = "id") Integer houseId, @PathVariable(name = "review_id") Integer reviewId, RedirectAttributes redirectAttributes) {
		reviewRepository.deleteById(reviewId);
		
		redirectAttributes.addFlashAttribute("successMessage", "レビューを削除しました。");
		
		return "redirect:/houses/" + houseId;
	}
}
