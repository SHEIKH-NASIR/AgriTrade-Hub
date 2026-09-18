
package com.softpro.ATH.Repository;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.FPOBulkOrder;
import com.softpro.ATH.Model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FPOBulkOrderRepo extends JpaRepository<FPOBulkOrder, Long> {

    List<FPOBulkOrder> findByFpo(FPO fpo);

    List<FPOBulkOrder> findByMerchant(Merchant merchant);

    List<FPOBulkOrder> findByFpoAndOrderStatus(
            FPO fpo,
            String orderStatus
    );

    List<FPOBulkOrder> findByMerchantAndOrderStatus(
            Merchant merchant,
            String orderStatus
    );

    long countByFpo(FPO fpo);

    long countByMerchant(Merchant merchant);
}

