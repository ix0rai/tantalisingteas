package io.ix0rai.tantalisingteas.data;

import io.ix0rai.tantalisingteas.TantalisingTeas;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.random.RandomGenerator;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class NbtUtil {
    private static final String INGREDIENTS_KEY = "Ingredients";
    private static final String ID_KEY = "id";
    private static final String FLAIR_KEY = "flair";
    private static final String COLOUR_KEY = "colour";
    private static final String NEEDS_UPDATE_KEY = "needsUpdate";
    private static final String STRENGTH_KEY = "strength";

    /**
     * gets the primary ingredient of the provided stack's nbt
     * @param stack the stack to get the primary ingredient of
     * @return the primary ingredient: the ingredient that occurs the most often in the nbt
     */
    public static NbtCompound getPrimaryIngredient(ItemStack stack) {
        if (stack.getNbt() == null) {
            return null;
        } else {
            NbtList ingredients = getIngredients(stack.getNbt());
            HashMap<NbtElement, Integer> counts = new HashMap<>();

            // count how many occurrences of each ingredient there are
            for (NbtElement ingredient : ingredients) {
                counts.put(ingredient, counts.getOrDefault(ingredient, 0) + 1);
            }

            return getElementWithHighestValue(counts);
        }
    }

    private static NbtCompound getElementWithHighestValue(Map<NbtElement, Integer> counts) {
        NbtElement primary = null;
        int number = 0;

        for (Map.Entry<NbtElement, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > number) {
                primary = entry.getKey();
            }
        }

        return (NbtCompound) primary;
    }

    /**
     * updates the custom stack name of the provided stack
     * @param stack the stack to update
     */
    public static void updateCustomName(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();

        if (nbt != null && needsUpdate(nbt)) {
            NbtCompound primaryIngredient = getPrimaryIngredient(stack);
            if (primaryIngredient != null) {
                String name = Util.translate(Util.BOTTLE) + " " + Util.translate(Util.OF) + " "
                        + Util.translate(Util.STRENGTHS[getOverallStrength(nbt)]) + " " + Util.translate(Registry.ITEM.get(new Identifier(primaryIngredient.getString(ID_KEY))).getTranslationKey()) + " " + Util.translate(Util.TEA);
                stack.setCustomName(Text.of(name));
            }
        }
    }

    public static Identifier getIngredientId(NbtCompound ingredient) {
        return new Identifier(ingredient.getString(ID_KEY));
    }

    public static TeaColour getColour(NbtCompound ingredient) {
        return TeaColour.get(ingredient.getString(COLOUR_KEY));
    }

    public static NbtList getIngredients(NbtCompound nbt) {
        if (nbt == null) {
            return new NbtList();
        } else {
            return nbt.getList(INGREDIENTS_KEY, 10);
        }
    }

    /**
     * gets the overall strength of the nbt's ingredient values
     * @param nbt the nbt to get the strength of
     * @return the overall strength of the nbt, formatted as an index of {@link Util#STRENGTHS}
     */
    private static int getOverallStrength(NbtCompound nbt) {
        NbtList ingredients = getIngredients(nbt);

        double averageStrength = 0;

        for (int i = 0; i < ingredients.size(); i++) {
            NbtCompound ingredient = ingredients.getCompound(i);
            averageStrength += ingredient.getInt(STRENGTH_KEY);
        }

        // calculate average strength
        return (int) Math.round(averageStrength / ingredients.size() / 2) - 1;
    }

    /**
     * gets the flair of a specific ingredient
     * @param nbt the full stack nbt to get the ingredient and flair data from
     * @param index the index of the ingredient
     * @return a formatted string for the flair of the ingredient
     */
    public static String getFlair(NbtCompound nbt, int index) {
        final NbtList ingredients = getIngredients(nbt);

        NbtCompound data = ingredients.getCompound(index);
        if (!data.isEmpty()) {
            return Util.getFlair(data.getInt(FLAIR_KEY));
        } else {
            return Util.getFlair(0);
        }
    }

    /**
     * adds an ingredient to the nbt of the given stack
     * @param stack the stack to add the ingredient to
     * @param ingredient the ingredient to add to the stack
     * @param random a random generator to generate a flair for the ingredient
     */
    public static void addIngredient(ItemStack stack, NbtCompound ingredient, RandomGenerator random) {
        if (ingredient != null && !ingredient.isEmpty() && ingredient.contains(ID_KEY)) {
            // get nbt
            NbtCompound nbt = stack.getOrCreateNbt();
            NbtList ingredients = getIngredients(nbt);

            // ensure item is in tea ingredient tag
            if (!isTeaIngredient(ingredient)) {
                TantalisingTeas.LOGGER.warn("attempted to add tea ingredient that is not in tea_ingredients tag; skipping");
                return;
            }

            // update nbt
            updateNbt(ingredient, random);

            // write nbt
            ingredients.add(ingredient);
            updateIngredients(ingredients, nbt);
            setUpdated(nbt);
        }
    }

    /**
     * since flair and strength can be automatically generated if they are not set,
     * this method sets those two values if they are not already present
     * @param ingredient an ingredient to update
     * @param random a random generator to use for generating flair
     */
    private static void updateNbt(NbtCompound ingredient, RandomGenerator random) {
        if (!ingredient.contains(FLAIR_KEY)) {
            setFlair(ingredient, random.nextInt(Util.FLAIRS.length));
        }

        if (!ingredient.contains(STRENGTH_KEY)) {
            setStrength(ingredient, 1);
        }
    }

    public static boolean isTeaIngredient(NbtCompound ingredient) {
        Identifier id = getIngredientId(ingredient);
        return Registry.ITEM.get(id).getDefaultStack().isIn(Util.TEA_INGREDIENTS);
    }

    public static void setNeedsUpdate(NbtCompound nbt) {
        setSafe(nbtCompound -> nbt.putBoolean(NEEDS_UPDATE_KEY, true), nbt);
    }

    public static void setUpdated(NbtCompound nbt) {
        setSafe(nbtCompound -> nbt.putBoolean(NEEDS_UPDATE_KEY, false), nbt);
    }

    public static boolean needsUpdate(NbtCompound nbt) {
        return nbt.getBoolean(NEEDS_UPDATE_KEY);
    }

    public static boolean hasColour(NbtCompound ingredient) {
        return ingredient.contains(COLOUR_KEY);
    }

    private static void setSafeWithReturn(Function<NbtCompound, NbtElement> function, NbtCompound nbt) {
        function.apply(createNbtIfNotPresent(nbt));
    }

    private static void setSafe(Consumer<NbtCompound> function, NbtCompound nbt) {
        function.accept(createNbtIfNotPresent(nbt));
    }

    private static NbtCompound createNbtIfNotPresent(NbtCompound nbt) {
        return Objects.requireNonNullElseGet(nbt, NbtCompound::new);
    }

    public static void updateIngredients(NbtList ingredients, NbtCompound nbt) {
        setSafeWithReturn(nbtCompound -> nbtCompound.put(INGREDIENTS_KEY, ingredients), nbt);
    }

    public static void setColour(NbtCompound ingredient, TeaColour colour) {
        setSafe(nbtCompound -> nbtCompound.putString(COLOUR_KEY, colour.getId()), ingredient);
    }

    public static void setStrength(NbtCompound ingredient, int strength) {
        setSafe(nbtCompound -> nbtCompound.putInt(STRENGTH_KEY, strength), ingredient);
    }

    public static void setFlair(NbtCompound ingredient, int flair) {
        setSafe(nbtCompound -> nbtCompound.putInt(FLAIR_KEY, flair), ingredient);
    }

    public static void setId(NbtCompound ingredient, Identifier id) {
        setSafe(nbtCompound -> nbtCompound.putString(ID_KEY, id.toString()), ingredient);
    }
}
