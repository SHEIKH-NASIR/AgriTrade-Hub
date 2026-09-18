package com.softpro.ATH.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.softpro.ATH.Model.Dispute;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;

public interface DisputeRepo extends JpaRepository<Dispute, Long> {
    List<Dispute> findAllByOrderByCreatedAtDesc();
    List<Dispute> findByMerchantOrderByCreatedAtDesc(Merchant merchant);
    Optional<Dispute> findByOrder(Order order);
}
