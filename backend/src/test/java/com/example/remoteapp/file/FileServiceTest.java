package com.example.remoteapp.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.file.dto.FileResponse;
import com.example.remoteapp.project.ProjectPermissionService;
import com.example.remoteapp.project.ProjectRole;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Unit test cho {@link FileService} — whitelist MIME, giới hạn size, quyền xoá.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
 */
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock private FileRepository fileRepository;
    @Mock private StorageService storageService;
    @Mock private ProjectPermissionService permissionService;

    private FileService fileService;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        fileService = new FileService(fileRepository, storageService, permissionService);
        actorId = UUID.randomUUID();
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H */
    @Test
    void storeAttachment_rejectsDisallowedMimeType() {
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe",
                "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> fileService.storeAttachment(actorId, file, null, null, null))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED));
        verify(storageService, never()).store(anyString(), any(), anyString());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H */
    @Test
    void storeAttachment_rejectsOversizeFile() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(11L * 1024 * 1024);

        assertThatThrownBy(() -> fileService.storeAttachment(actorId, file, null, null, null))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_TOO_LARGE));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A4 */
    @Test
    void storeAvatar_rejectsNonImage() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf",
                "pdf".getBytes());

        assertThatThrownBy(() -> fileService.storeAvatar(actorId, file))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H */
    @Test
    void storeAttachment_persistsMetadataAndStorageKey() {
        UUID projectId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "hello".getBytes());
        when(fileRepository.save(any(FileMeta.class))).thenAnswer(invocation -> {
            FileMeta saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        FileResponse response = fileService.storeAttachment(actorId, file, projectId, null, null);

        assertThat(response.name()).isEqualTo("note.txt");
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.url()).startsWith("/api/v1/files/");
        verify(permissionService).requireRole(projectId, actorId, ProjectRole.MEMBER);
        verify(storageService).store(anyString(), any(byte[].class), eq("text/plain"));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H */
    @Test
    void delete_rejectsOtherUserInProject() {
        UUID fileId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        FileMeta meta = new FileMeta();
        meta.setId(fileId);
        meta.setOwnerId(UUID.randomUUID());
        meta.setProjectId(projectId);
        meta.setStorageKey("key");
        when(fileRepository.findById(fileId)).thenReturn(java.util.Optional.of(meta));
        when(permissionService.requireRole(projectId, actorId, ProjectRole.ADMIN))
                .thenThrow(new AppException(ErrorCode.PROJECT_FORBIDDEN));

        assertThatThrownBy(() -> fileService.delete(actorId, fileId))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROJECT_FORBIDDEN));
        verify(storageService, never()).delete(anyString());
    }
}
