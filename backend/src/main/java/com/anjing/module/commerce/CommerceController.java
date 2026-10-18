package com.anjing.module.commerce;

import com.anjing.model.result.R;
import com.anjing.module.commerce.CommerceDTO.*;
import com.anjing.module.commerce.CommerceVO.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 单商家本地演示接口；生产身份授权由会话/认证模块接入，见 commerce-contract.md。 */
@RestController
@RequestMapping("/api/commerce")
@RequiredArgsConstructor
public class CommerceController {
    private final CommerceService service;
    private final CommerceDemoService demo;
    @PostMapping("/orders/query")
    public R<OrderMatches> orders(@Valid @RequestBody OrderQuery request) { return R.ok(service.getOrder(request)); }
    @PostMapping("/logistics/query")
    public R<LogisticsResult> logistics(@Valid @RequestBody LogisticsQuery request) { return R.ok(service.getLogistics(request)); }
    @PostMapping("/tickets/create")
    public R<TicketCreated> create(@Valid @RequestBody CreateTicket request) { return R.ok(service.createTicket(request)); }
    @PostMapping("/tickets/query")
    public R<List<Ticket>> tickets(@Valid @RequestBody TicketQuery request) { return R.ok(service.getServiceTicket(request)); }
    @PostMapping("/tickets/update")
    public R<Ticket> update(@Valid @RequestBody UpdateTicket request) { return R.ok(service.updateTicket(request)); }
    @PostMapping("/demo/identities")
    public R<DemoIdentities> identities() { return R.ok(demo.identities()); }
    @PostMapping("/demo/load")
    public R<DemoRestored> load() { return R.ok(demo.load()); }
    @PostMapping("/demo/restore")
    public R<DemoRestored> restore(@Valid @RequestBody RestoreDemo request) { return R.ok(demo.restore(request.confirmed())); }
}
