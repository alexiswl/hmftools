package com.hartwig.hmftools.vicc.datamodel;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class MolecularMatchAstRightLeftRight {

    @Nullable
    public abstract String raw();

    @NotNull
    public abstract String type();

    @Nullable
    public abstract String value();
}
