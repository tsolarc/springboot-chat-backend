package com.Backend.User.controller;

import com.Backend.Media.DTO.MediaDTO;
import com.Backend.Media.service.IMediaService;
import com.Backend.User.dto.MusicGenreDTO;
import com.Backend.User.dto.UserDTO;
import com.Backend.User.entity.User;
import com.Backend.User.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/login")
@SessionAttributes("userDTO")
public class LoginController {

    @Autowired
    private IAuthService authService;

    @Autowired
    private IMusicGenreService musicGenreService;

    @Autowired
    private UserProfileServiceImpl userProfileService;

    @Autowired
    private IMediaService mediaService;

    @ModelAttribute("userDTO")
    public UserDTO userDTO() {
        return new UserDTO();
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute UserDTO userDTO, HttpSession session, Model model) {
        try {
            UserDTO registered = authService.register(userDTO);
            session.setAttribute("email", registered.getEmail());
            return "redirect:/auth/name";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @PostMapping("/name")
    public String saveName(@ModelAttribute("userDTO") UserDTO dto, HttpSession session) {
        if (dto.getEmail() == null) {
            String email = (String) session.getAttribute("email");
            dto.setEmail(email);
        }
        return "redirect:/auth/username";
    }

    @PostMapping("/username")
    public String saveUsername(@ModelAttribute("userDTO") UserDTO profileDTO, @RequestParam(value = "username", required = false) String username) {
        profileDTO.setUsername(username);
        return "redirect:/auth/location";
    }

    @PostMapping("/location")
    public String saveLocation(@ModelAttribute("userDTO") UserDTO profileDTO, @RequestParam("location") String location) {
        profileDTO.setLocation(location);
        return "redirect:/auth/biography";
    }

    @PostMapping("/biography")
    public String saveBiography(@ModelAttribute("userDTO") UserDTO profileDTO,
                                @RequestParam("biography") String biography) {
        profileDTO.setBiography(biography);
        return "redirect:/auth/socialNetwork";
    }

    @PostMapping("/socialNetwork")
    public String saveSocialNetworks(@ModelAttribute("userDTO") UserDTO profileDTO, @RequestParam(value = "socialNetworks", required = false) List<String> networks) {
        if (networks != null) {
            List<String> cleanedNetworks = networks.stream()
                    .filter(n -> n != null && !n.trim().isEmpty())
                    .collect(Collectors.toList());
            profileDTO.setSocialNetworks(cleanedNetworks);
        } else {
            profileDTO.setSocialNetworks(new ArrayList<>());
        }
        return "redirect:/auth/musicGenres";
    }

    @PostMapping("/musicGenres")
    public String saveMusicGenres(@ModelAttribute("userDTO") UserDTO profileDTO) {

        List<Long> musicGenresIds = profileDTO.getMusicGenresIds();

        Set<MusicGenreDTO> genreDTOs = musicGenresIds != null
                ? musicGenresIds.stream()
                .map(musicGenreService::findDTOById)
                .collect(Collectors.toSet())
                : new HashSet<>();

        profileDTO.setMusicGenres(genreDTOs);

        return "redirect:/auth/profilePhoto";
    }

    @PostMapping("/profilePhoto")
    public String saveProfilePhoto(@RequestParam("profilePhoto") MultipartFile file, @ModelAttribute("userDTO") UserDTO profileDTO, RedirectAttributes redirectAttributes) {
        User idUser = userProfileService.getUserByEmail(profileDTO.getEmail());
        try {
            MediaDTO media = mediaService.uploadMedia(file, idUser.getId());

            profileDTO.setProfilePhotoUrl(media.getFileUrl());

            return "redirect:/auth/finish";

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Hubo un error al subir la imagen.");
            return "redirect:/auth/profilePhoto";
        }
    }
}
