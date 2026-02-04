package com.raindropcentral.rplatform.reward;

public final class CoreRewardTypes {

    private CoreRewardTypes() {}

    public static void registerAll() {
        BuiltInRewardProvider.getInstance().register();
    }
}
