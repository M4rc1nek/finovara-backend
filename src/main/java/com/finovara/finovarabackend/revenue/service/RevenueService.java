package com.finovara.finovarabackend.revenue.service;

import com.finovara.finovarabackend.exception.RevenueNotFoundException;
import com.finovara.finovarabackend.exception.UserNotFoundException;
import com.finovara.finovarabackend.exception.WalletNotFoundException;
import com.finovara.finovarabackend.revenue.dto.RevenueDTO;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import com.finovara.finovarabackend.wallet.service.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final UserRepository userRepository;
    private final RevenueRepository revenueRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;

    public User getUserEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public Revenue getRevenueOrThrow(Long revenueId) {
        return revenueRepository.findById(revenueId)
                .orElseThrow(() -> new RevenueNotFoundException("Revenue not found"));
    }

    @Transactional
    public RevenueDTO editRevenue(RevenueDTO revenueDTO, Long revenueId, String email) {
        Revenue existingRevenue = getRevenueOrThrow(revenueId);
        User user = getUserEmailOrThrow(email);

        if (!existingRevenue.getUserAssigned().getId().equals(user.getId())) {
            throw new RevenueNotFoundException("Revenue not found for this user");
        }

        Wallet wallet = walletRepository.findByUserAssignedEmail(email)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        wallet.setBalance(wallet.getBalance().subtract(existingRevenue.getAmount()));
        wallet.setBalance(wallet.getBalance().add(revenueDTO.amount()));
        walletRepository.save(wallet);

        existingRevenue.setAmount(revenueDTO.amount());
        existingRevenue.setCategory(revenueDTO.category());
        existingRevenue.setCreatedAt(LocalDate.now());
        existingRevenue.setDescription(revenueDTO.description());

        revenueRepository.save(existingRevenue);

        return new RevenueDTO(
                existingRevenue.getId(),
                existingRevenue.getUserAssigned().getId(),
                existingRevenue.getAmount(),
                existingRevenue.getCategory(),
                existingRevenue.getCreatedAt(),
                existingRevenue.getDescription()
        );
    }

    @Transactional
    public RevenueDTO addRevenue(RevenueDTO revenueDTO, String email) {
        User user = getUserEmailOrThrow(email);

        Revenue revenue = Revenue.builder()
                .amount(revenueDTO.amount())
                .category(revenueDTO.category())
                .createdAt(LocalDate.now())
                .description(revenueDTO.description())
                .userAssigned(user)
                .build();
        walletService.addBalanceToWallet(email, revenue.getAmount());
        //wallet jest zapisywany w repo klasie WalletService
        revenueRepository.save(revenue);

        return new RevenueDTO(
                revenue.getId(),
                user.getId(),
                revenue.getAmount(),
                revenue.getCategory(),
                revenue.getCreatedAt(),
                revenue.getDescription()
        );
    }

    public List<RevenueDTO> getRevenue(String email) {
        User user = getUserEmailOrThrow(email);
        List<Revenue> revenue = revenueRepository.findAllByUserAssignedId(user.getId());

        return revenue.stream()
                .map(this::mapRevenueToDTO)
                .toList();
    }

    public RevenueDTO mapRevenueToDTO(Revenue revenue) {
        return new RevenueDTO(
                revenue.getId(),
                revenue.getUserAssigned().getId(),
                revenue.getAmount(),
                revenue.getCategory(),
                revenue.getCreatedAt(),
                revenue.getDescription()
        );
    }


    @Transactional
    public void deleteRevenue(Long revenueId, String email) {
        User user = getUserEmailOrThrow(email);
        Revenue revenue = revenueRepository.findByIdAndUserAssignedId(revenueId, user.getId())
                .orElseThrow(() -> new RevenueNotFoundException("Revenue not found"));
        walletService.removeBalanceFromWallet(email, revenue.getAmount());
        revenueRepository.delete(revenue);

    }


}
