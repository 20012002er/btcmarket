package com.lazybeartoby.btcmarket.service;

import com.lazybeartoby.btcmarket.common.constant.TransactionType;
import com.lazybeartoby.btcmarket.common.exception.BizException;
import com.lazybeartoby.btcmarket.model.entity.Wallet;
import com.lazybeartoby.btcmarket.model.entity.WalletTransaction;
import com.lazybeartoby.btcmarket.model.vo.TransactionVO;
import com.lazybeartoby.btcmarket.model.vo.WalletVO;
import com.lazybeartoby.btcmarket.repository.BetRepository;
import com.lazybeartoby.btcmarket.repository.WalletRepository;
import com.lazybeartoby.btcmarket.repository.WalletTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository txRepository;
    private final BetRepository betRepository;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository txRepository,
                         BetRepository betRepository) {
        this.walletRepository = walletRepository;
        this.txRepository = txRepository;
        this.betRepository = betRepository;
    }

    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUserId(userId);
            w.setBalance(BigDecimal.ZERO);
            w.setFrozenBalance(BigDecimal.ZERO);
            return walletRepository.save(w);
        });
    }

    public WalletVO getWalletVO(Long userId) {
        Wallet w = getOrCreateWallet(userId);
        WalletVO vo = new WalletVO();
        vo.setUserId(userId);
        vo.setBalance(w.getBalance());
        vo.setFrozenBalance(w.getFrozenBalance());
        vo.setTotalProfit(betRepository.profitByUser(userId));
        return vo;
    }

    @Transactional
    public void freezeForBet(Long userId, BigDecimal amount, Long betId) {
        Wallet w = getOrCreateWallet(userId);
        if (w.getBalance().compareTo(amount) < 0) {
            throw new BizException("可用积分不足");
        }
        w.setBalance(w.getBalance().subtract(amount));
        w.setFrozenBalance(w.getFrozenBalance().add(amount));
        walletRepository.save(w);

        recordTx(userId, TransactionType.BET_FREEZE, amount, 2, w, betId, "BET", "押注冻结积分");
    }

    @Transactional
    public void settleWin(Long userId, BigDecimal returnAmount, BigDecimal profit, Long betId, String remark) {
        Wallet w = getOrCreateWallet(userId);
        w.setFrozenBalance(w.getFrozenBalance().subtract(returnAmount));
        w.setBalance(w.getBalance().add(returnAmount).add(profit));
        walletRepository.save(w);

        recordTx(userId, TransactionType.BET_WIN, profit, 1, w, betId, "BET", remark);
    }

    @Transactional
    public void settleLoss(Long userId, BigDecimal lostAmount, Long betId, String remark) {
        Wallet w = getOrCreateWallet(userId);
        w.setFrozenBalance(w.getFrozenBalance().subtract(lostAmount));
        walletRepository.save(w);

        recordTx(userId, TransactionType.BET_LOSS, lostAmount, 2, w, betId, "BET", remark);
    }

    @Transactional
    public void refundBet(Long userId, BigDecimal amount, Long betId, String remark) {
        Wallet w = getOrCreateWallet(userId);
        w.setFrozenBalance(w.getFrozenBalance().subtract(amount));
        w.setBalance(w.getBalance().add(amount));
        walletRepository.save(w);

        recordTx(userId, TransactionType.BET_REFUND, amount, 1, w, betId, "BET", remark);
    }

    @Transactional
    public void adminGrant(Long userId, BigDecimal amount, String remark, Long adminLogId, boolean deduct) {
        Wallet w = getOrCreateWallet(userId);
        if (deduct) {
            if (w.getBalance().compareTo(amount) < 0) {
                throw new BizException("用户余额不足，无法扣减");
            }
            w.setBalance(w.getBalance().subtract(amount));
            walletRepository.save(w);
            recordTx(userId, TransactionType.ADMIN_DEDUCT, amount, 2, w, adminLogId, "ADMIN", remark);
        } else {
            w.setBalance(w.getBalance().add(amount));
            walletRepository.save(w);
            recordTx(userId, TransactionType.ADMIN_GRANT, amount, 1, w, adminLogId, "ADMIN", remark);
        }
    }

    private void recordTx(Long userId, TransactionType type, BigDecimal amount, int direction,
                          Wallet w, Long referenceId, String referenceType, String remark) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setDirection(direction);
        tx.setBalanceAfter(w.getBalance());
        tx.setFrozenAfter(w.getFrozenBalance());
        tx.setReferenceId(referenceId);
        tx.setReferenceType(referenceType);
        tx.setRemark(remark);
        txRepository.save(tx);
    }

    public Page<TransactionVO> transactions(Long userId, int page, int size) {
        return txRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toVO);
    }

    private TransactionVO toVO(WalletTransaction tx) {
        TransactionVO vo = new TransactionVO();
        vo.setId(tx.getId());
        vo.setType(tx.getType());
        vo.setAmount(tx.getAmount());
        vo.setDirection(tx.getDirection());
        vo.setBalanceAfter(tx.getBalanceAfter());
        vo.setFrozenAfter(tx.getFrozenAfter());
        vo.setRemark(tx.getRemark());
        vo.setCreatedAt(tx.getCreatedAt());
        return vo;
    }
}
