package com.finovara.activitylogservice.activitylog.mainaccount.wallet.mapper;

import com.finovara.activitylogservice.activitylog.mainaccount.wallet.dto.WalletStatusActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.model.WalletStatusActivity;
import org.springframework.stereotype.Component;

@Component
public class WalletStatusActivityMapper {

    public WalletStatusActivityDto mapToDto(WalletStatusActivity activity) {
        return new WalletStatusActivityDto(
                activity.getType(),
                activity.getReason(),
                activity.getAmount(),
                activity.getCreatedAt()
        );
    }
}
