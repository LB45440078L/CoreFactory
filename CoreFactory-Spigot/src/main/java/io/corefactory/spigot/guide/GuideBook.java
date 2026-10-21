package io.corefactory.spigot.guide;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.research.ResearchTree;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The tutorial, as written books. The basics come first, then a contents page for the science chapters, then the
 * chapters themselves, all generated from the loaded data by {@link ScienceGuide}. A written book holds at most
 * {@link #MAX_PAGES} pages, so the guide is given as one book, or as several when it would not fit in one.
 */
public final class GuideBook {

    /** The most pages a written book holds. */
    public static final int MAX_PAGES = 100;

    private static final String TITLE = "CoreFactory Guide";

    private static final List<String> BASICS = List.of(
            "&6&lCoreFactory Guide\n\n&7Everything here is also in /cf help.\n\n"
                    + "&e1 &7Crafting\n&e2 &7Placing and lighting\n&e3 &7Fuse linker\n&e4 &7Components\n"
                    + "&e5 &7Nuclear weapons\n&e6 &7Achievements\n&e7 &7Commands\n&e8 &7Science",
            "&6Crafting\n\n&7Each TNT has a recipe. Open /cf gui for the catalogue.\n\n"
                    + "Right-click an entry to see its recipe in a 3 by 3 grid, or run /cf recipe <id>.\n\n"
                    + "The strongest TNT needs components.",
            "&6Placing and lighting\n\n&7Place a custom TNT as you would normal TNT.\n\n"
                    + "Light it with flint and steel, a fire charge, or redstone power.\n\n"
                    + "A lit TNT runs its own mechanic when it blasts.",
            "&6Fuse linker\n\n&7Right-click a placed TNT with the fuse linker, then right-click a second one.\n\n"
                    + "Linked TNT chains: one blast sets off the others.\n\n"
                    + "Sneak and right-click to clear the links.",
            "&6Components\n\n&7Components are crafted first, then used in the strongest recipes.\n\n"
                    + "Nuclear weapons need enriched uranium and plutonium pits.\n\n"
                    + "Check any recipe with /cf recipe <id>.",
            "&6Nuclear weapons\n\n&7A nuclear TNT counts down, then blasts.\n\n"
                    + "Get more than 1.5 times its radius away, or get underground.\n\n"
                    + "Players nearby see beams, daytime and no rain for a while.",
            "&6Achievements\n\n&7Crafting, lighting, detonating, chains and nuclear feats all count.\n\n"
                    + "Each unlock is announced in chat.\n\nOpen /cf achievements to see your progress.",
            "&6Commands\n\n&7/cf gui  catalogue\n/cf recipe <id>\n/cf achievements\n/cf book\n"
                    + "/cf info  placed TNT\n/cf aim, target, unlink\n/cf help");

    private final CoreFactoryCore core;
    private final ResearchTree tree;
    private final ProcessCatalog processes;
    private final List<OreDefinition> ores;

    /**
     * @param core      the loaded TNT types and components, read each time a book is given
     * @param tree      the research tree the chapters describe
     * @param processes the machines and processes the chapters describe
     * @param ores      the ores the chapters describe
     */
    public GuideBook(CoreFactoryCore core, ResearchTree tree, ProcessCatalog processes, List<OreDefinition> ores) {
        this.core = core;
        this.tree = tree;
        this.processes = processes;
        this.ores = List.copyOf(ores);
    }

    /** Every page of the guide, in reading order. */
    public List<String> pages() {
        ScienceGuide.Source source = new ScienceGuide.Source(tree, processes, ores, core.definitions(),
                core.componentDefinitions());
        List<ScienceGuide.Chapter> chapters = ScienceGuide.chapters(source);
        List<String> pages = new ArrayList<>(BASICS);
        pages.addAll(PageSplitter.paginate(contentsParagraphs(chapters)));
        for (ScienceGuide.Chapter chapter : chapters) {
            pages.addAll(PageSplitter.paginate(chapter.paragraphs()));
        }
        return pages.stream().map(Messages::color).toList();
    }

    /**
     * The guide as written books, each holding at most {@link #MAX_PAGES} pages. The first book is the whole guide
     * when it fits, which it does for the shipped data.
     */
    public List<ItemStack> books() {
        List<String> pages = pages();
        List<ItemStack> books = new ArrayList<>();
        for (int start = 0; start < pages.size(); start += MAX_PAGES) {
            int number = books.size() + 1;
            String title = number == 1 ? TITLE : TITLE + " " + number;
            books.add(book(title, pages.subList(start, Math.min(pages.size(), start + MAX_PAGES))));
        }
        return books;
    }

    private static List<String> contentsParagraphs(List<ScienceGuide.Chapter> chapters) {
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add("&6&lScience");
        paragraphs.add("");
        paragraphs.add("&7The chapters, in order:");
        paragraphs.add("");
        for (ScienceGuide.Chapter chapter : chapters) {
            paragraphs.add("&7" + chapter.title());
        }
        return paragraphs;
    }

    private static ItemStack book(String title, List<String> pages) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle(title);
            meta.setAuthor("CoreFactory");
            meta.setGeneration(BookMeta.Generation.ORIGINAL);
            meta.setPages(pages);
            book.setItemMeta(meta);
        }
        return book;
    }
}
