package de.jexcellence.home.utility.heads;

import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * House head used for home-related UI elements.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class House extends RHead {

    public static final String HOUSE_UUID = "7d7075a9-1df3-485d-bf00-ffd6ce2d6244";
    public static final String HOUSE_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzc0MTBjMDdiZmJiNDE0NTAwNGJmOTE4YzhkNjMwMWJkOTdjZTEzMjcwY2UxZjIyMWQ5YWFiZWUxYWZkNTJhMyJ9fX0=";

    public House() {
        super("house", HOUSE_UUID, HOUSE_TEXTURE);
    }
}
