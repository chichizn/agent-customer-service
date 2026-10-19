package com.anjing.module.chat.workbench;

import java.util.regex.Pattern;

/** Shared by conversation routing and the final write guard. Never accept a substring as confirmation. */
final class ConfirmationText {
    private static final Pattern CONFIRM = Pattern.compile("确认提交(?:退货诉求|退货申请|申请|诉求|工单)?[！!。]?");
    private ConfirmationText() {}
    static boolean matches(String text) { return text != null && CONFIRM.matcher(text.trim()).matches(); }
}
