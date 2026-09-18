package com.softpro.ATH.Service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;

@Service
public class DemoAccessService {

    public void requireMerchant(Merchant merchant) {
        if (merchant == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Merchant session expired.");
        }
        if (!merchant.isDemoEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demo access is not enabled for this merchant.");
        }
    }

    public void requireFpo(FPO fpo) {
        if (fpo == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "FPO session expired.");
        }
        if (!fpo.isDemoEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demo access is not enabled for this FPO.");
        }
    }

    public void requireFarmer(Formers farmer) {
        if (farmer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Farmer session expired.");
        }
        if (!farmer.isDemoEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demo access is not enabled for this farmer.");
        }
    }
}
