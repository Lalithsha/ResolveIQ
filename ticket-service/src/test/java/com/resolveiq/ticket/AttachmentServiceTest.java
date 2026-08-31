package com.resolveiq.ticket;

import com.resolveiq.ticket.application.port.AttachmentStoragePort;
import com.resolveiq.ticket.application.port.MalwareScannerPort;
import com.resolveiq.ticket.application.service.AttachmentService;
import com.resolveiq.ticket.domain.exception.UnsafeAttachmentException;
import com.resolveiq.ticket.domain.model.Ticket;
import com.resolveiq.ticket.domain.model.TicketAttachment;
import com.resolveiq.ticket.domain.model.TicketPriority;
import com.resolveiq.ticket.domain.repository.TicketAttachmentRepository;
import com.resolveiq.ticket.domain.repository.TicketRepository;
import com.resolveiq.ticket.domain.repository.StaffTeamMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {
    @Mock TicketRepository tickets;
    @Mock TicketAttachmentRepository attachments;
    @Mock AttachmentStoragePort storage;
    @Mock MalwareScannerPort scanner;
    @Mock StaffTeamMembershipRepository memberships;
    @Mock MultipartFile file;
    private AttachmentService service;
    private UUID tenantId;
    private UUID customerId;
    private UUID ticketId;

    @BeforeEach
    void setUp() {
        service = new AttachmentService(tickets, attachments, storage, scanner, memberships, true, 1024, 5);
        tenantId = UUID.randomUUID(); customerId = UUID.randomUUID(); ticketId = UUID.randomUUID();
        Ticket ticket = new Ticket(ticketId, "RIQ-TEST-1", tenantId, customerId, "Evidence", "Attached evidence", "GENERAL", TicketPriority.MEDIUM, "WEB", "en");
        lenient().when(tickets.findByIdAndTenantIdAndCustomerId(ticketId, tenantId, customerId)).thenReturn(Optional.of(ticket));
        lenient().when(attachments.countByTenantIdAndTicketId(tenantId, ticketId)).thenReturn(0L);
    }

    @Test
    void scansBeforeStoringAndPersistsTheContentHash() throws Exception {
        byte[] content = "customer evidence".getBytes(StandardCharsets.UTF_8);
        when(file.isEmpty()).thenReturn(false); when(file.getSize()).thenReturn((long) content.length);
        when(file.getContentType()).thenReturn("text/plain"); when(file.getOriginalFilename()).thenReturn("../evidence.txt");
        when(file.getBytes()).thenReturn(content); when(scanner.scan("evidence.txt", content)).thenReturn(new MalwareScannerPort.ScanResult(true, "clean"));
        when(scanner.engineName()).thenReturn("test-scanner");
        when(attachments.save(any(TicketAttachment.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.uploadCustomer(tenantId, customerId, ticketId, file);

        assertThat(result.fileName()).isEqualTo("evidence.txt");
        assertThat(result.sha256()).hasSize(64);
        var order = inOrder(scanner, storage, attachments);
        order.verify(scanner).scan("evidence.txt", content);
        order.verify(storage).put(anyString(), eq("text/plain"), eq(content));
        order.verify(attachments).save(any(TicketAttachment.class));
    }

    @Test
    void rejectsMalwareWithoutWritingObjectStorage() throws Exception {
        byte[] content = "EICAR".getBytes(StandardCharsets.UTF_8);
        when(file.isEmpty()).thenReturn(false); when(file.getSize()).thenReturn((long) content.length);
        when(file.getContentType()).thenReturn("text/plain"); when(file.getOriginalFilename()).thenReturn("sample.txt");
        when(file.getBytes()).thenReturn(content); when(scanner.scan("sample.txt", content)).thenReturn(new MalwareScannerPort.ScanResult(false, "signature"));

        assertThatThrownBy(() -> service.uploadCustomer(tenantId, customerId, ticketId, file))
            .isInstanceOf(UnsafeAttachmentException.class);
        verifyNoInteractions(storage);
        verify(attachments, never()).save(any());
    }
}
