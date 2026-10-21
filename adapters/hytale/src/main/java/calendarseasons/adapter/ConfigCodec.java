package calendarseasons.adapter;

import calendarseasons.core.CalendarDef;
import calendarseasons.core.Settings;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code config.json} as the server's own config codec sees it. The field order here is the order the codec
 * writes. Keys follow the codec's rule: PascalCase.
 */
final class ConfigCodec {
    private ConfigCodec() {}

    static final class Month {
        String name = "";
        int days = 1;
        String season = "";

        static final BuilderCodec<Month> CODEC = BuilderCodec.builder(Month.class, Month::new)
            .append(new KeyedCodec<>("Name", Codec.STRING), (o, v) -> o.name = v, o -> o.name).add()
            .append(new KeyedCodec<>("Days", Codec.INTEGER), (o, v) -> o.days = v, o -> o.days).add()
            .append(new KeyedCodec<>("Season", Codec.STRING), (o, v) -> o.season = v, o -> o.season).add()
            .build();
    }

    static final class Cal {
        String preset;
        long epochYear;
        String yearLabel;
        Month[] customMonths;
        String[] customWeekdays;

        static final BuilderCodec<Cal> CODEC = BuilderCodec.builder(Cal.class, Cal::new)
            .append(new KeyedCodec<>("Preset", Codec.STRING), (o, v) -> o.preset = v, o -> o.preset).add()
            .append(new KeyedCodec<>("EpochYear", Codec.LONG), (o, v) -> o.epochYear = v, o -> o.epochYear).add()
            .append(new KeyedCodec<>("YearLabel", Codec.STRING), (o, v) -> o.yearLabel = v, o -> o.yearLabel).add()
            .append(new KeyedCodec<>("CustomMonths", new ArrayCodec<>(Month.CODEC, Month[]::new)), (o, v) -> o.customMonths = v, o -> o.customMonths).add()
            .append(new KeyedCodec<>("CustomWeekdays", Codec.STRING_ARRAY), (o, v) -> o.customWeekdays = v, o -> o.customWeekdays).add()
            .build();

        Cal() {
            Settings d = Settings.defaults();
            preset = d.preset();
            epochYear = d.epochYear();
            yearLabel = d.yearLabel();
            customMonths = d.customMonths().stream().map(m -> {
                Month x = new Month();
                x.name = m.name();
                x.days = m.days();
                x.season = m.season();
                return x;
            }).toArray(Month[]::new);
            customWeekdays = d.customWeekdays().toArray(String[]::new);
        }
    }

    static final class Clk {
        String mode;
        double calendarDaysPerWorldDay;
        long realTimeEpochMillis;
        double utcOffsetHours;

        static final BuilderCodec<Clk> CODEC = BuilderCodec.builder(Clk.class, Clk::new)
            .append(new KeyedCodec<>("Mode", Codec.STRING), (o, v) -> o.mode = v, o -> o.mode).add()
            .append(new KeyedCodec<>("CalendarDaysPerWorldDay", Codec.DOUBLE), (o, v) -> o.calendarDaysPerWorldDay = v, o -> o.calendarDaysPerWorldDay).add()
            .append(new KeyedCodec<>("RealTimeEpochMillis", Codec.LONG), (o, v) -> o.realTimeEpochMillis = v, o -> o.realTimeEpochMillis).add()
            .append(new KeyedCodec<>("UtcOffsetHours", Codec.DOUBLE), (o, v) -> o.utcOffsetHours = v, o -> o.utcOffsetHours).add()
            .build();

        Clk() {
            Settings d = Settings.defaults();
            mode = d.mode();
            calendarDaysPerWorldDay = d.calendarDaysPerWorldDay();
            realTimeEpochMillis = d.realTimeEpochMillis();
            utcOffsetHours = d.utcOffsetHours();
        }
    }

    static final class Ann {
        boolean atDawn;
        boolean seasonChange;
        String template;

        static final BuilderCodec<Ann> CODEC = BuilderCodec.builder(Ann.class, Ann::new)
            .append(new KeyedCodec<>("AtDawn", Codec.BOOLEAN), (o, v) -> o.atDawn = v, o -> o.atDawn).add()
            .append(new KeyedCodec<>("SeasonChange", Codec.BOOLEAN), (o, v) -> o.seasonChange = v, o -> o.seasonChange).add()
            .append(new KeyedCodec<>("Template", Codec.STRING), (o, v) -> o.template = v, o -> o.template).add()
            .build();

        Ann() {
            Settings d = Settings.defaults();
            atDawn = d.announceAtDawn();
            seasonChange = d.announceSeasonChange();
            template = d.announceTemplate();
        }
    }

    static final class Disp {
        String dateFormat;
        boolean showWeekday;
        boolean showSeason;
        Ann announce = new Ann();

        static final BuilderCodec<Disp> CODEC = BuilderCodec.builder(Disp.class, Disp::new)
            .append(new KeyedCodec<>("DateFormat", Codec.STRING), (o, v) -> o.dateFormat = v, o -> o.dateFormat).add()
            .append(new KeyedCodec<>("ShowWeekday", Codec.BOOLEAN), (o, v) -> o.showWeekday = v, o -> o.showWeekday).add()
            .append(new KeyedCodec<>("ShowSeason", Codec.BOOLEAN), (o, v) -> o.showSeason = v, o -> o.showSeason).add()
            .append(new KeyedCodec<>("Announce", Ann.CODEC), (o, v) -> o.announce = v, o -> o.announce).add()
            .build();

        Disp() {
            Settings d = Settings.defaults();
            dateFormat = d.dateFormat();
            showWeekday = d.showWeekday();
            showSeason = d.showSeason();
        }
    }

    /** Kept inside config.json only by the local test variant. */
    static final class St {
        long startDay;
        String lastSeason;

        static final BuilderCodec<St> CODEC = BuilderCodec.builder(St.class, St::new)
            .append(new KeyedCodec<>("StartDay", Codec.LONG), (o, v) -> o.startDay = v, o -> o.startDay).add()
            .append(new KeyedCodec<>("LastSeason", Codec.STRING), (o, v) -> o.lastSeason = v, o -> o.lastSeason).add()
            .build();
    }

    static final class Root {
        int version = Settings.VERSION;
        Cal calendar = new Cal();
        Clk clock = new Clk();
        Disp display = new Disp();
        St state;
    }

    /** The codec for config.json; with {@code withState} it also has the State object. */
    static BuilderCodec<Root> codec(boolean withState) {
        BuilderCodec.Builder<Root> b = BuilderCodec.builder(Root.class, Root::new);
        b.append(new KeyedCodec<>("Version", Codec.INTEGER), (o, v) -> o.version = v, o -> o.version).add();
        b.append(new KeyedCodec<>("Calendar", Cal.CODEC), (o, v) -> o.calendar = v, o -> o.calendar).add();
        b.append(new KeyedCodec<>("Clock", Clk.CODEC), (o, v) -> o.clock = v, o -> o.clock).add();
        b.append(new KeyedCodec<>("Display", Disp.CODEC), (o, v) -> o.display = v, o -> o.display).add();
        if (withState) b.append(new KeyedCodec<>("State", St.CODEC), (o, v) -> o.state = v, o -> o.state).add();
        return b.build();
    }

    static Settings toSettings(Root r) {
        Cal c = r.calendar == null ? new Cal() : r.calendar;
        Clk k = r.clock == null ? new Clk() : r.clock;
        Disp d = r.display == null ? new Disp() : r.display;
        Ann a = d.announce == null ? new Ann() : d.announce;
        List<CalendarDef.Month> months = new ArrayList<>();
        if (c.customMonths != null) for (Month m : c.customMonths) months.add(m == null ? null : new CalendarDef.Month(m.name, m.days, m.season));
        List<String> weekdays = c.customWeekdays == null ? List.of() : new ArrayList<>(Arrays.asList(c.customWeekdays));
        return new Settings(r.version, c.preset, c.epochYear, c.yearLabel, nullFree(months), weekdays, k.mode, k.calendarDaysPerWorldDay,
            k.realTimeEpochMillis, k.utcOffsetHours, d.dateFormat, d.showWeekday, d.showSeason, a.atDawn, a.seasonChange, a.template);
    }

    /** List.copyOf refuses nulls; an empty list element becomes a nameless month, which validation reports. */
    private static List<CalendarDef.Month> nullFree(List<CalendarDef.Month> l) {
        List<CalendarDef.Month> out = new ArrayList<>();
        for (CalendarDef.Month m : l) out.add(m == null ? new CalendarDef.Month(null, 0, null) : m);
        return out;
    }
}
