package com.raindropcentral.rplatform.utility.heads.view;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Pagination control head representing navigation to a higher page index.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
public class Up extends RHead {

    /**
     * Identifier used to resolve pagination translations.
     */
    private static final String IDENTIFIER = "up";

    /**
     * UUID string associated with the upward navigation head.
     */
    private static final String UUID = "45c0cd71-7ca9-2288-4965-2a8ccdf5281c";

    /**
     * Base64-encoded texture representing the up arrow icon.
     */
    private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDVjMGNkNzE3Y2E5MjI4ODQ5NjUyYThjY2RmNTI4MWE4NjBjYjJlNzk2NDZiNTZhMjJiYzU5MTEwZjM2MWRhIn19fQ==";

    /**
     * Creates the up navigation head definition with translation key {@code head.up}.
     */
    public Up() {
        super(
            IDENTIFIER,
            UUID,
            TEXTURE
        );
    }
}
