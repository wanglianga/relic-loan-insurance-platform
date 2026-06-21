package com.relic.platform.service;

import com.relic.platform.entity.DamageRecord;
import com.relic.platform.entity.ReturnRecord;
import com.relic.platform.entity.User;
import com.relic.platform.repository.DamageRecordRepository;
import com.relic.platform.repository.ReturnRecordRepository;
import com.relic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReturnService {

    @Autowired
    private ReturnRecordRepository returnRecordRepository;

    @Autowired
    private DamageRecordRepository damageRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<ReturnRecord> list(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<ReturnRecord> returnRecordPage = returnRecordRepository.findByStatus(status, pageable);
        returnRecordPage.forEach(this::loadBasicRelations);
        return returnRecordPage;
    }

    @Transactional(readOnly = true)
    public ReturnRecord getById(Long id) {
        ReturnRecord returnRecord = returnRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("归还记录不存在"));
        loadBasicRelations(returnRecord);
        loadDamageRecords(returnRecord);
        return returnRecord;
    }

    private void loadBasicRelations(ReturnRecord returnRecord) {
        if (returnRecord.getReceivedBy() != null) {
            Optional<User> receiver = userRepository.findById(returnRecord.getReceivedBy());
            receiver.ifPresent(u -> returnRecord.setReceiverName(u.getName()));
        }
    }

    private void loadDamageRecords(ReturnRecord returnRecord) {
        List<DamageRecord> damageRecords = damageRecordRepository.findByReturnId(returnRecord.getId());
        returnRecord.setDamageRecords(damageRecords);
    }

    @Transactional
    public ReturnRecord create(ReturnRecord record) {
        record.setId(null);
        record.setOverallStatus("IN_TRANSIT");
        return returnRecordRepository.save(record);
    }

    @Transactional
    public ReturnRecord receive(Long id, ReturnRecord record, Long receiverId) {
        ReturnRecord existing = getById(id);
        if (!"IN_TRANSIT".equals(existing.getOverallStatus()) && !"ARRIVED".equals(existing.getOverallStatus())) {
            throw new RuntimeException("归还记录状态不允许接收");
        }
        existing.setReturnNotes(record.getReturnNotes());
        existing.setReceivedBy(receiverId);
        existing.setReceivedAt(LocalDateTime.now());
        existing.setOverallStatus("RECEIVED");
        return returnRecordRepository.save(existing);
    }

    @Transactional
    public DamageRecord addDamage(Long returnId, DamageRecord damage) {
        ReturnRecord returnRecord = getById(returnId);
        if (!"RECEIVED".equals(returnRecord.getOverallStatus())) {
            throw new RuntimeException("归还记录状态不允许添加损坏记录");
        }
        damage.setId(null);
        damage.setReturnId(returnId);
        damage.setRecordedAt(LocalDateTime.now());
        return damageRecordRepository.save(damage);
    }
}
