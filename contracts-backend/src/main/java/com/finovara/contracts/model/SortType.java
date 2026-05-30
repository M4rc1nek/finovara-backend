package com.finovara.contracts.model;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public enum SortType {
    NEWEST {
        @Override
        public Pageable getPageable(int pageSize) {
            return PageRequest.of(0, pageSize, Sort.by("createdAt").descending());
        }
    },
    OLDEST {
        @Override
        public Pageable getPageable(int pageSize) {
            return PageRequest.of(0, pageSize, Sort.by("createdAt").ascending());
        }
    },
    AMOUNT_DESC {
        @Override
        public Pageable getPageable(int pageSize) {
            return PageRequest.of(0, pageSize, Sort.by("amount").descending());
        }
    },
    AMOUNT_ASC {
        @Override
        public Pageable getPageable(int pageSize) {
            return PageRequest.of(0, pageSize, Sort.by("amount").ascending());
        }
    };

    public abstract Pageable getPageable(int pageSize);
}