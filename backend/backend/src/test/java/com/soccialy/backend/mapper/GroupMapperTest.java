package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GroupMapperTest {

    private GroupMapper groupMapper;

    @BeforeEach
    void setUp() {
        groupMapper = new GroupMapper();
    }

    @Test
    void toDTO_Success() {
        User member = new User();
        member.setId(2);
        member.setUsername("member");

        Group group = new Group();
        group.setId(10);
        group.setName("Test Grup");
        group.setUsers(new HashSet<>(Set.of(member)));

        GroupDTO dto = groupMapper.toDTO(group);

        assertNotNull(dto);
        assertEquals(10, dto.getId());
        assertEquals("Test Grup", dto.getName());
        assertEquals(1, dto.getMemberCount());
    }

    @Test
    void toDTO_NullGroup_ReturnsNull() {
        assertNull(groupMapper.toDTO(null));
    }

    @Test
    void toDTO_NoUsers_ReturnsZeroMemberCount() {
        Group group = new Group();
        group.setId(5);
        group.setName("Empty Group");

        GroupDTO dto = groupMapper.toDTO(group);

        assertNotNull(dto);
        assertEquals(5, dto.getId());
        assertEquals("Empty Group", dto.getName());
        assertEquals(0, dto.getMemberCount());
    }

    @Test
    void toEntity_Success() {
        GroupDTO dto = new GroupDTO();
        dto.setId(10);
        dto.setName("DTO Grup");

        Group entity = groupMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(10, entity.getId());
        assertEquals("DTO Grup", entity.getName());
    }

    @Test
    void toEntity_NullDTO_ReturnsNull() {
        assertNull(groupMapper.toEntity(null));
    }
}
