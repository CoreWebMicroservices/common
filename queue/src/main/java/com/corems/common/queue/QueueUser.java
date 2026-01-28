package com.corems.common.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private Collection<String> roles;
}
