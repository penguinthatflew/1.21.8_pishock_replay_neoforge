package net.ptf.tutorialmod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = TutorialMod.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = TutorialMod.MODID, value = Dist.CLIENT)
public class ExampleModClient {
    public ExampleModClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        TutorialMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        TutorialMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    // Note that we can not default this value to 20F, because what if the player
    // logs onto a server and is already at 10 health?
    private static float last_health = -1;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        // invalid player (we are not in survival/adventure) do nothing
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) {
            last_health = -1;
            return;
        }

        float curr_health = mc.player.getHealth();


        // our current health hasn't been populated.
        if (last_health == -1) {
            last_health = curr_health;
            return;
        }

        // We are healing or damage is dealt on absorption; do nothing
        if (curr_health >= last_health || curr_health >= 20) {
            last_health = curr_health;
            return;
        }

        if (last_health - curr_health < 0.5) {
            //damage taken less than half a heart, do nothing
            last_health = curr_health;
            return;
        }

        if (curr_health == 0.0) {
            last_health = curr_health;
            doDeathPunishment();
            return;
        }


        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("[PTF]: Damage taken:" + last_health + " -> " + curr_health));

        shock(calculateIntensity(curr_health,last_health),Config.DURATION.getAsInt());

        last_health = curr_health;
    }

    static void doDeathPunishment() {
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("[PTF]: You died!"));
        shock(Config.INTENSITY.getAsInt(), Config.DURATION.getAsInt());
    }


    private static final ExecutorService SHOCK_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "PTF-Shock");
                t.setDaemon(true);
                return t;
            });


    //We want to use a single client because .build is expensive (~200+ ms)
    private static final HttpClient CLIENT =
            HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

    static void shockBlocking(int intensity, int duration) throws Exception {
        String json = String.format(
                """
                {
                  "Username":"%s",
                  "Name":"appname",
                  "Code":"%s",
                  "Intensity":%d,
                  "Duration":%d,
                  "Apikey":"%s",
                  "Op":%d
                }
                """,
                Config.USERNAME.get(),
                Config.SHARE_CODE.get(),
                intensity,
                duration,
                Config.API_KEY.get(),
                Config.OP_MODE.get().ordinal()
        );
        TutorialMod.LOGGER.info("API KEY: " + Config.API_KEY.get());
        TutorialMod.LOGGER.info("SHARE CODE: "+ Config.SHARE_CODE.get());
        TutorialMod.LOGGER.info("USERNAME: " + Config.USERNAME.get());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://do.pishock.com/api/apioperate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // We send asynchronously to prevent subsequent shocks from self-throttling/going over the last one
        CLIENT.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                .thenAccept(resp -> {
                    int code = resp.statusCode();
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().gui.getChat().addMessage(
                                Component.literal("Code: " + code));
                    });
                });
    }

    //Async function
    static void shock(int intensity, int duration) {
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("[PTF]: Shocking for intensity: " + intensity));

        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("[PTF]: Shocking for duration: " + duration));


        SHOCK_EXECUTOR.execute(() -> {
            try {
                shockBlocking(intensity, duration);
            } catch (Exception e) {
            }
        });
    }

    static int calculateIntensity(float curr_health, float last_health) {
        float damage_taken = (curr_health - last_health) * -1;
        return (int) ((damage_taken / 20.F) * Config.INTENSITY.getAsInt());
    }
}
