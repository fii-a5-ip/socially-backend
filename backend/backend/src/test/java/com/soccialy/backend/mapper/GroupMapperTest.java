package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.dto.GroupUserDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.GroupUser;
import com.soccialy.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GroupMapper — covers Entity-to-DTO and DTO-to-Entity conversions.
 */
class GroupMapperTest {

    private GroupMapper groupMapper;

    @BeforeEach
    void setUp() {
        groupMapper = new GroupMapper();
    }

    @Test
    void toDTO_Success() {
        // Arrange
        User creator = new User();
        creator.setId(1);
        creator.setUsername("creator");

        User member = new User();
        member.setId(2);
        member.setUsername("member");

        Group group = new Group();
        group.setId(10);
        group.setName("Test Grup");
        group.setDesc("Test Descriere");
        group.setImgLink("https://example.com/img.png");
        group.setCreator(creator);

        GroupUser gu1 = new GroupUser();
        gu1.setGroup(group);
        gu1.setUser(creator);
        gu1.setRole("ADMIN");

        GroupUser gu2 = new GroupUser();
        gu2.setGroup(group);
        gu2.setUser(member);
        gu2.setRole("MEMBER");

        group.setGroupUsers(new HashSet<>(Set.of(gu1, gu2)));

        // Act
        GroupDTO dto = groupMapper.toDTO(group);

        // Assert
        assertNotNull(dto);
        assertEquals(10, dto.getId());
        assertEquals("Test Grup", dto.getName());
        assertEquals("Test Descriere", dto.getDesc());
        assertEquals("https://example.com/img.png", dto.getImgLink());
        assertEquals(1, dto.getCreatorUserId());

        assertNotNull(dto.getMembers());
        assertEquals(2, dto.getMembers().size());

        boolean hasAdmin = dto.getMembers().stream()
                .anyMatch(m -> m.getUserId() == 1 && "ADMIN".equals(m.getRole()));
        boolean hasMember = dto.getMembers().stream()
                .anyMatch(m -> m.getUserId() == 2 && "MEMBER".equals(m.getRole()));

        assertTrue(hasAdmin);
        assertTrue(hasMember);
    }

    @Test
    void toDTO_NullGroup_ReturnsNull() {
        assertNull(groupMapper.toDTO(null));
    }

    @Test
    void toDTO_NullCreatorAndUsers() {
        Group group = new Group();
        group.setId(5);
        group.setName("Empty Group");

        GroupDTO dto = groupMapper.toDTO(group);

        assertNotNull(dto);
        assertEquals(5, dto.getId());
        assertEquals("Empty Group", dto.getName());
        assertNull(dto.getCreatorUserId());
        // Group entity initializes groupUsers with empty HashSet (@Builder.Default),
        // so members will be an empty list, not null
        assertNotNull(dto.getMembers());
        assertTrue(dto.getMembers().isEmpty());
    }

    @Test
    void toDTO_NullGroupUsers_ReturnsNullMembers() {
        Group group = new Group();
        group.setId(6);
        group.setName("No Members");
        group.setGroupUsers(null);

        GroupDTO dto = groupMapper.toDTO(group);

        assertNotNull(dto);
        assertEquals(6, dto.getId());
        assertEquals("No Members", dto.getName());
        assertNull(dto.getMembers());
    }

    @Test
    void toEntity_Success() {
        // Arrange
        GroupDTO dto = new GroupDTO();
        dto.setId(10);
        dto.setName("DTO Grup");
        dto.setDesc("DTO Descriere");
        dto.setImgLink("https://example.com/img.png");

        // Act
        Group entity = groupMapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals(10, entity.getId());
        assertEquals("DTO Grup", entity.getName());
        assertEquals("DTO Descriere", entity.getDesc());
        assertEquals("https://example.com/img.png", entity.getImgLink());
    }

    @Test
    void toEntity_NullDTO_ReturnsNull() {
        assertNull(groupMapper.toEntity(null));
    }
}
