package net.coding.ide.dto;

import lombok.Data;

/**
 * Created by tan on 09/03/2017.
 */
@Data
public class Package {

    public enum Requirement {
        Optional, Required
    }

    private String name;

    private String version;

    private String description;

    private String author;

    private String displayName;

    private Requirement requirement;

}
