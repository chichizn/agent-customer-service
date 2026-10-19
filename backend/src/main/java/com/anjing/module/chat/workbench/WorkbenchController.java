package com.anjing.module.chat.workbench;

import com.anjing.model.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
public class WorkbenchController {
    private final WorkbenchService workbench;
    @PostMapping("/api/chat/session/open")
    public R<WorkbenchVO.Snapshot> open(@Valid @RequestBody WorkbenchDTO.Open request) { return R.ok(workbench.open(request)); }
    @PostMapping("/api/chat/session/snapshot")
    public R<WorkbenchVO.Snapshot> snapshot(@Valid @RequestBody WorkbenchDTO.SessionId request) { return R.ok(workbench.fetch(request)); }
    @PostMapping("/api/chat/message/human/send")
    public R<WorkbenchVO.Snapshot> human(@Valid @RequestBody WorkbenchDTO.Send request) { return R.ok(workbench.human(request)); }
    @PostMapping("/api/chat/session/reception/update")
    public R<WorkbenchVO.Snapshot> reception(@Valid @RequestBody WorkbenchDTO.Reception request) { return R.ok(workbench.reception(request)); }
    @PostMapping("/api/demo/reset")
    public R<Void> reset(@Valid @RequestBody WorkbenchDTO.Reset request) { workbench.reset(request); return R.ok(); }
}
