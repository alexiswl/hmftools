package com.hartwig.hmftools.vicc.datamodel;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class GenePmkb {
    @Nullable
    public abstract String description();

    @NotNull
    public abstract String createdAt();

    @NotNull
    public abstract String updatedAt();

    @NotNull
    public abstract String activeInd();

    @NotNull
    public abstract String externalId();

    @NotNull
    public abstract String id();

    @NotNull
    public abstract String name();

}
