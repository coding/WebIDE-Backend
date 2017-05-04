package net.coding.ide.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by tan on 03/05/2017.
 */
@Data
public class GitBlame {

    private PersonIdent author;

    private String shortName;

    @Data
    @AllArgsConstructor
    public static class PersonIdent {
        private String name;

        private String emailAddress;

        private Long when;
    }
}
