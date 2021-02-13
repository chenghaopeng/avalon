package cn.chper.avalon.service.game;

public class Role {

    public static final Integer RedJoker = 12;

    public static final Integer RedKing = 11;

    public static final Integer RedA = 10;

    public static final Integer BlackJoker = 3;

    public static final Integer Black2 = 2;

    public static final Integer BlackA = 1;

    public Integer type;

    public boolean good;

    public Role(Integer type) {
        this.type = type;
        this.good = isGood(this);
    }

    public static final Integer[][] roles = {
        null,
        null,
        null,
        null,
        null,
        null,
        new Integer[]{Role.RedJoker, Role.RedKing, Role.RedA, Role.RedA, Role.BlackJoker, Role.Black2},
        new Integer[]{Role.RedJoker, Role.RedKing, Role.RedA, Role.RedA, Role.BlackJoker, Role.Black2, Role.BlackA},
        new Integer[]{Role.RedJoker, Role.RedKing, Role.RedA, Role.RedA, Role.RedA, Role.BlackJoker, Role.Black2, Role.BlackA}
    };

    public static boolean isGood(Role r) {
        return r.type >= RedA;
    }

    public static boolean isBad(Role r) {
        return !isGood(r);
    }

    public static boolean isNormal(Role r) {
        return r.type == RedA || r.type == BlackA;
    }

    public String toString() {
        if (this.type == RedJoker) return "红-大王";
        if (this.type == RedKing) return "红-K";
        if (this.type == RedA) return "红-A";
        if (this.type == BlackJoker) return "黑-小王";
        if (this.type == Black2) return "黑-2";
        if (this.type == BlackA) return "黑-A";
        return null;
    }

}
