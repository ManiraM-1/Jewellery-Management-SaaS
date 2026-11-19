package com.jewelerp.service;

import com.jewelerp.model.ActivityLog;
import com.jewelerp.model.AppUser;
import com.jewelerp.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final ActivityLogRepository logs;

    public void log(AppUser user, String module, String action, String description) {
        ActivityLog entry = new ActivityLog();
        entry.setUser(user);
        entry.setModule(module);
        entry.setAction(action);
        entry.setDescription(description);
        logs.save(entry);
    }
}
