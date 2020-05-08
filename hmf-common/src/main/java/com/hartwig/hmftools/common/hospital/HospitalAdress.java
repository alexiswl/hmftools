package com.hartwig.hmftools.common.hospital;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class HospitalAdress {

    @NotNull
    public abstract String hospitalId();

    @NotNull
    public abstract String hospitalName();

    @NotNull
    public abstract String hospitalZip();

    @NotNull
    public abstract String hospitalCity();
}
