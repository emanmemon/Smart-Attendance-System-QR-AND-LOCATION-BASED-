package com.smartattendance.app;

import android.content.Context;
import android.content.Intent;

public class RoleRouter {
    public static void goToDashboard(Context ctx, String role) {
        Class<?> target = "teacher".equals(role)
                ? TeacherDashboardActivity.class
                : StudentDashboardActivity.class;
        Intent i = new Intent(ctx, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(i);
    }
}
