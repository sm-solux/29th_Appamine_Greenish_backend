package com.solux.greenish.User.Service;

import com.solux.greenish.Photo.Domain.Photo;
import com.solux.greenish.Photo.Dto.PhotoResponseDto;
import com.solux.greenish.Photo.Repository.PhotoRepository;
import com.solux.greenish.Photo.Service.PhotoService;
import com.solux.greenish.User.Dto.UserDto.*;
import com.solux.greenish.User.Repository.UserRepository;
import com.solux.greenish.User.Domain.User;
import com.solux.greenish.login.Jwt.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;


import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final PhotoRepository photoRepository;
    private final PhotoService photoService;
    private final JwtUtil jwtUtil;

    private Photo findPhotoById(Long photoId) {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사진을 조회할 수 없습니다."));
    }

    public boolean isEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    private Photo getPhoto(Long photoId) {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사진을 조회할 수 없습니다. "));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 조회할 수 없습니다. "));
    }

    public boolean isNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Transactional
    public IdResponse signUp(@Valid UserRegistDto request) {
        if (isEmailDuplicate(request.getEmail())) {
            throw new RuntimeException("이미 가입되어 있는 이메일입니다. ");
        }
        if (isNicknameDuplicate(request.getNickname())) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }

        User user = request.toUser(bCryptPasswordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        if (request.getFileName() != null) {
            PhotoResponseDto photo = photoService.generatePreSignedDto(user.getId(), request.getFileName());
            user.updatePhoto(getPhoto(photo.getPhotoId()));
        }

        return IdResponse.of(user);
    }

    @Transactional
    public void deleteAccount(String token) {
        String email = jwtUtil.getEmail(token.split(" ")[1]);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원을 조회할 수 없습니다."));
        Photo photo = user.getPhoto();
        photoService.deletePhoto(photo);
        userRepository.deleteById(user.getId());
    }

    @Transactional(readOnly = true)
    public UserInfoDto getUserInfo(String token) {
        String email = jwtUtil.getEmail(token.split(" ")[1]);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원을 조회할 수 없습니다."));

        return UserInfoDto.of(user, photoService.getFilePath(user.getPhoto()));
    }

    @Transactional(readOnly = true)
    public List<UserInfoDto> getAllUserInfo() {
        List<User> users = userRepository.findAll();
        return userRepository.findAll().stream()
                .map((user) -> UserInfoDto.of(user, photoService.getFilePath(user.getPhoto()))).toList();
    }


}
