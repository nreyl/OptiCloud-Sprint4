package com.opticloud.reports.web;

import com.opticloud.reports.command.CommandService;
import com.opticloud.reports.command.CreateReportCommand;
import com.opticloud.reports.domain.Report;
import com.opticloud.reports.query.ReportView;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports/commands")
public class CommandController {

    private final CommandService commands;

    public CommandController(CommandService commands) {
        this.commands = commands;
    }

    @PostMapping("/reports")
    public ResponseEntity<ReportView> create(@RequestBody @Valid CreateReportCommand command) {
        Report saved = commands.createReport(command);
        ReportView view = ReportView.from(saved);
        return ResponseEntity.created(URI.create("/reports/queries/reports/" + view.id())).body(view);
    }

    @DeleteMapping("/reports/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        commands.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
