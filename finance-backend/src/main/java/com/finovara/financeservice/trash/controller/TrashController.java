package com.finovara.financeservice.trash.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.trash.dto.TrashFilterDto;
import com.finovara.financeservice.trash.dto.TrashItemDto;
import com.finovara.financeservice.trash.service.TrashService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trash")
@RequiredArgsConstructor
@Validated
public class TrashController {

    private final TrashService trashService;

    @GetMapping
    public ResponseEntity<List<TrashItemDto>> getTrash(@RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(trashService.getTrash(SecurityUtils.getCurrentUserId(), page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<TrashItemDto>> filterTrash(@ModelAttribute TrashFilterDto filter, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(trashService.filterTrash(SecurityUtils.getCurrentUserId(), filter, page));
    }

    @PostMapping("/{trashItemId}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long trashItemId, @RequestParam(required = false) String authorizationCode) {
        trashService.restore(SecurityUtils.getCurrentUserId(), trashItemId, authorizationCode);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{trashItemId}")
    public ResponseEntity<Void> permanentlyDelete(@PathVariable Long trashItemId, @RequestParam(required = false) String authorizationCode) {
        trashService.permanentlyDelete(SecurityUtils.getCurrentUserId(), trashItemId, authorizationCode);
        return ResponseEntity.noContent().build();
    }
}
