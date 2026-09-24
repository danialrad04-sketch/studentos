package com.example.data.seed

import com.example.data.local.entity.CurriculumCourseEntity

object CurriculumSeedData {
    val chemicalEngineeringCourses: List<CurriculumCourseEntity> = listOf(
        // ترم 1
        CurriculumCourseEntity("CE_MATH_1", "CHEM_ENG", "1115101", "ریاضی عمومی 1", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CE_PHYS_1", "CHEM_ENG", "1115102", "فیزیک 1 (مکانیک)", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CE_CHEM_GEN", "CHEM_ENG", "1115103", "شیمی عمومی", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CE_CHEM_GEN_LAB", "CHEM_ENG", "1115104", "آزمایشگاه شیمی عمومی", 1, "آزمایشگاهی", 1, coRequisites = "شیمی عمومی"),
        CurriculumCourseEntity("CE_PROG", "CHEM_ENG", "1115105", "برنامه‌نویسی کامپیوتر (پایتون)", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CE_PERSIAN", "CHEM_ENG", "1115106", "فارسی عمومی", 2, "عمومی", 1, ""),
        CurriculumCourseEntity("CE_ENG_DRAW", "CHEM_ENG", "1115107", "نقشه‌کشی صنعتی", 2, "پایه", 1, ""),

        // ترم 2
        CurriculumCourseEntity("CE_MATH_2", "CHEM_ENG", "1115201", "ریاضی عمومی 2", 3, "پایه", 2, "ریاضی عمومی 1"),
        CurriculumCourseEntity("CE_PHYS_2", "CHEM_ENG", "1115202", "فیزیک 2 (الکتریسیته و مغناطیس)", 3, "پایه", 2, "فیزیک 1"),
        CurriculumCourseEntity("CE_PHYS_LAB_1", "CHEM_ENG", "1115203", "آزمایشگاه فیزیک 1", 1, "آزمایشگاهی", 2, "فیزیک 1"),
        CurriculumCourseEntity("CE_DIFF_EQ", "CHEM_ENG", "1115204", "معادلات دیفرانسیل", 3, "پایه", 2, "ریاضی عمومی 1"),
        CurriculumCourseEntity("CE_ORG_CHEM", "CHEM_ENG", "1115205", "شیمی آلی 1", 3, "تخصصی", 2, "شیمی عمومی"),
        CurriculumCourseEntity("CE_ENG_LANG", "CHEM_ENG", "1115206", "زبان خارجی عمومی", 2, "عمومی", 2, ""),
        CurriculumCourseEntity("CE_ISLAMIC_THOUGHT_1", "CHEM_ENG", "1115207", "اندیشه اسلامی 1", 2, "عمومی", 2, ""),

        // ترم 3
        CurriculumCourseEntity("CE_MAT_BAL", "CHEM_ENG", "1115301", "موازنه انرژی و مواد", 3, "تخصصی", 3, "شیمی عمومی"),
        CurriculumCourseEntity("CE_THERMO_1", "CHEM_ENG", "1115302", "ترمودینامیک 1", 3, "تخصصی", 3, "فیزیک 1، ریاضی عمومی 2"),
        CurriculumCourseEntity("CE_PHYS_CHEM_1", "CHEM_ENG", "1115303", "شیمی فیزیک 1", 3, "تخصصی", 3, "شیمی عمومی، ریاضی عمومی 2"),
        CurriculumCourseEntity("CE_STATICS", "CHEM_ENG", "1115304", "استاتیک و مقاومت مصالح", 3, "پایه", 3, "فیزیک 1"),
        CurriculumCourseEntity("CE_NUM_METH", "CHEM_ENG", "1115305", "روش‌های محاسبات عددی", 2, "پایه", 3, "معادلات دیفرانسیل"),
        CurriculumCourseEntity("CE_PHYS_LAB_2", "CHEM_ENG", "1115306", "آزمایشگاه فیزیک 2", 1, "آزمایشگاهی", 3, "فیزیک 2"),
        CurriculumCourseEntity("CE_ETHICS", "CHEM_ENG", "1115307", "اخلاق اسلامی", 2, "عمومی", 3, ""),

        // ترم 4
        CurriculumCourseEntity("CE_FLUID_1", "CHEM_ENG", "1115401", "مکانیک سیالات 1", 3, "تخصصی", 4, "موازنه انرژی و مواد، معادلات دیفرانسیل"),
        CurriculumCourseEntity("CE_HEAT_1", "CHEM_ENG", "1115402", "انتقال حرارت 1", 3, "تخصصی", 4, "موازنه انرژی و مواد، معادلات دیفرانسیل"),
        CurriculumCourseEntity("CE_THERMO_2", "CHEM_ENG", "1115403", "ترمودینامیک 2", 3, "تخصصی", 4, "ترمودینامیک 1"),
        CurriculumCourseEntity("CE_ENG_MATH", "CHEM_ENG", "1115404", "ریاضیات مهندسی", 3, "پایه", 4, "معادلات دیفرانسیل"),
        CurriculumCourseEntity("CE_PHYS_CHEM_LAB", "CHEM_ENG", "1115405", "آزمایشگاه شیمی فیزیک", 1, "آزمایشگاهی", 4, "شیمی فیزیک 1"),
        CurriculumCourseEntity("CE_ORG_CHEM_LAB", "CHEM_ENG", "1115406", "آزمایشگاه شیمی آلی", 1, "آزمایشگاهی", 4, "شیمی آلی 1"),
        CurriculumCourseEntity("CE_ISLAMIC_THOUGHT_2", "CHEM_ENG", "1115407", "اندیشه اسلامی 2", 2, "عمومی", 4, "اندیشه اسلامی 1"),

        // ترم 5
        CurriculumCourseEntity("CE_MASS_1", "CHEM_ENG", "1115501", "انتقال جرم 1", 3, "تخصصی", 5, "ترمودینامیک 2، انتقال حرارت 1"),
        CurriculumCourseEntity("CE_HEAT_2", "CHEM_ENG", "1115502", "انتقال حرارت 2", 2, "تخصصی", 5, "انتقال حرارت 1"),
        CurriculumCourseEntity("CE_FLUID_2", "CHEM_ENG", "1115503", "مکانیک سیالات 2", 2, "تخصصی", 5, "مکانیک سیالات 1"),
        CurriculumCourseEntity("CE_KINETICS", "CHEM_ENG", "1115504", "سینتیک و طراحی رآکتور 1", 3, "تخصصی", 5, "ترمودینامیک 2"),
        CurriculumCourseEntity("CE_UNIT_OP_LAB_1", "CHEM_ENG", "1115505", "آزمایشگاه عملیات واحد 1", 1, "آزمایشگاهی", 5, "مکانیک سیالات 1، انتقال حرارت 1"),
        CurriculumCourseEntity("CE_MATERIAL_SCI", "CHEM_ENG", "1115506", "شناخت و انتخاب مواد مهندسی", 2, "تخصصی", 5, "شیمی فیزیک 1"),

        // ترم 6
        CurriculumCourseEntity("CE_MASS_2", "CHEM_ENG", "1115601", "عملیات واحد 1 (جداسازی)", 3, "تخصصی", 6, "انتقال جرم 1"),
        CurriculumCourseEntity("CE_PROC_CONTROL_1", "CHEM_ENG", "1115602", "کنترل فرآیندها 1", 3, "تخصصی", 6, "ریاضیات مهندسی، مکانیک سیالات 1"),
        CurriculumCourseEntity("CE_REACT_2", "CHEM_ENG", "1115603", "سینتیک و طراحی رآکتور 2", 2, "تخصصی", 6, "طراحی رآکتور 1"),
        CurriculumCourseEntity("CE_PROC_SIM", "CHEM_ENG", "1115604", "کاربرد نرم‌افزارهای شبیه‌سازی فرآیند (اسپن هایسیس)", 2, "تخصصی", 6, "ترمودینامیک 2، انتقال جرم 1"),
        CurriculumCourseEntity("CE_UNIT_OP_LAB_2", "CHEM_ENG", "1115605", "آزمایشگاه عملیات واحد 2", 1, "آزمایشگاهی", 6, "عملیات واحد 1"),

        // ترم 7
        CurriculumCourseEntity("CE_PROC_DESIGN_1", "CHEM_ENG", "1115701", "طراحی فرآیندهای شیمیایی 1", 3, "تخصصی", 7, "عملیات واحد 1، رآکتور 2"),
        CurriculumCourseEntity("CE_PROC_CONTROL_LAB", "CHEM_ENG", "1115702", "آزمایشگاه کنترل فرآیند", 1, "آزمایشگاهی", 7, "کنترل فرآیندها 1"),
        CurriculumCourseEntity("CE_CORROSION", "CHEM_ENG", "1115703", "خوردگی در صنایع شیمیایی", 2, "تخصصی", 7, "شناخت مواد"),
        CurriculumCourseEntity("CE_SAFETY_ENV", "CHEM_ENG", "1115704", "ایمنی، بهداشت و محیط زیست (HSE)", 2, "تخصصی", 7, ""),
        CurriculumCourseEntity("CE_INTERNSHIP", "CHEM_ENG", "1115705", "کارآموزی تابستان", 2, "تخصصی", 7, "گذراندن 80 واحد"),

        // ترم 8
        CurriculumCourseEntity("CE_PROC_DESIGN_2", "CHEM_ENG", "1115801", "طراحی فرآیند 2 و پروژه کارشناسی", 3, "تخصصی", 8, "طراحی فرآیند 1"),
        CurriculumCourseEntity("CE_ECONOMICS", "CHEM_ENG", "1115802", "اقتصاد و بهینه‌سازی فرآیندها", 2, "تخصصی", 8, "طراحی فرآیند 1"),
        CurriculumCourseEntity("CE_REFINERY_PETRO", "CHEM_ENG", "1115803", "فرآیندهای پالایش نفت و پتروشیمی", 3, "تخصصی", 8, "عملیات واحد 1"),
        CurriculumCourseEntity("CE_POLYMERS", "CHEM_ENG", "1115804", "اصول مهندسی پلیمر", 2, "تخصصی", 8, "شیمی آلی 1")
    )

    val computerEngineeringCourses: List<CurriculumCourseEntity> = listOf(
        // ترم 1
        CurriculumCourseEntity("CS_MATH_1", "COMP_ENG", "2115101", "ریاضی عمومی 1", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CS_PHYS_1", "COMP_ENG", "2115102", "فیزیک 1 (مکانیک)", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CS_PROG_FND", "COMP_ENG", "2115103", "مبانی برنامه‌نویسی (پایتون/C++)", 3, "تخصصی", 1, ""),
        CurriculumCourseEntity("CS_PROG_LAB", "COMP_ENG", "2115104", "کارگاه برنامه‌نویسی", 1, "آزمایشگاهی", 1, coRequisites = "مبانی برنامه‌نویسی"),
        CurriculumCourseEntity("CS_DISCRETE", "COMP_ENG", "2115105", "ساختارهای گسسته", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CS_PERSIAN", "COMP_ENG", "2115106", "فارسی عمومی", 2, "عمومی", 1, ""),
        // ترم 2
        CurriculumCourseEntity("CS_MATH_2", "COMP_ENG", "2115201", "ریاضی عمومی 2", 3, "پایه", 2, "ریاضی عمومی 1"),
        CurriculumCourseEntity("CS_PHYS_2", "COMP_ENG", "2115202", "فیزیک 2 (الکتریسیته)", 3, "پایه", 2, "فیزیک 1"),
        CurriculumCourseEntity("CS_DIFF_EQ", "COMP_ENG", "2115203", "معادلات دیفرانسیل", 3, "پایه", 2, "ریاضی عمومی 1"),
        CurriculumCourseEntity("CS_ADV_PROG", "COMP_ENG", "2115204", "برنامه‌نویسی پیشرفته (جاوا/کاتلین)", 3, "تخصصی", 2, "مبانی برنامه‌نویسی"),
        CurriculumCourseEntity("CS_DIGITAL_LOGIC", "COMP_ENG", "2115205", "مدارهای منطقی", 3, "تخصصی", 2, "ساختارهای گسسته"),
        CurriculumCourseEntity("CS_DIGITAL_LAB", "COMP_ENG", "2115206", "آزمایشگاه مدارهای منطقی", 1, "آزمایشگاهی", 2, coRequisites = "مدارهای منطقی"),
        // ترم 3
        CurriculumCourseEntity("CS_DATA_STRUCT", "COMP_ENG", "2115301", "ساختمان داده‌ها و الگوریتم‌ها", 3, "تخصصی", 3, "برنامه‌نویسی پیشرفته، ساختارهای گسسته"),
        CurriculumCourseEntity("CS_ELEC_CIRC", "COMP_ENG", "2115302", "مدارهای الکتریکی", 3, "تخصصی", 3, "فیزیک 2، معادلات دیفرانسیل"),
        CurriculumCourseEntity("CS_ARCH_1", "COMP_ENG", "2115303", "معماری کامپیوتر", 3, "تخصصی", 3, "مدارهای منطقی"),
        CurriculumCourseEntity("CS_STAT_PROB", "COMP_ENG", "2115304", "آمار و احتمال مهندسی", 3, "پایه", 3, "ریاضی عمومی 2"),
        CurriculumCourseEntity("CS_ENG_LANG", "COMP_ENG", "2115305", "زبان تخصصی مهندسی کامپیوتر", 2, "تخصصی", 3, ""),
        CurriculumCourseEntity("CS_ISLAMIC_1", "COMP_ENG", "2115306", "اندیشه اسلامی 1", 2, "عمومی", 3, ""),
        // ترم 4
        CurriculumCourseEntity("CS_ALGO_DESIGN", "COMP_ENG", "2115401", "طراحی الگوریتم‌ها", 3, "تخصصی", 4, "ساختمان داده‌ها"),
        CurriculumCourseEntity("CS_OS", "COMP_ENG", "2115402", "سیستم‌های عامل", 3, "تخصصی", 4, "ساختمان داده‌ها، معماری کامپیوتر"),
        CurriculumCourseEntity("CS_OS_LAB", "COMP_ENG", "2115403", "آزمایشگاه سیستم‌های عامل", 1, "آزمایشگاهی", 4, coRequisites = "سیستم‌های عامل"),
        CurriculumCourseEntity("CS_MICROPROC", "COMP_ENG", "2115404", "ریزپردازنده‌ها و زبان اسمبلی", 3, "تخصصی", 4, "معماری کامپیوتر"),
        CurriculumCourseEntity("CS_DATABASE", "COMP_ENG", "2115405", "پایگاه داده‌ها", 3, "تخصصی", 4, "ساختمان داده‌ها"),
        CurriculumCourseEntity("CS_NUM_ANALYSIS", "COMP_ENG", "2115406", "محاسبات عددی", 2, "پایه", 4, "معادلات دیفرانسیل"),
        // ترم 5
        CurriculumCourseEntity("CS_NETWORKS", "COMP_ENG", "2115501", "شبکه‌های کامپیوتری 1", 3, "تخصصی", 5, "سیستم‌های عامل"),
        CurriculumCourseEntity("CS_AI", "COMP_ENG", "2115502", "هوش مصنوعی و یادگیری ماشین", 3, "تخصصی", 5, "طراحی الگوریتم‌ها"),
        CurriculumCourseEntity("CS_THEORY_COMP", "COMP_ENG", "2115503", "نظریه زبان‌ها و اتوماتا", 3, "تخصصی", 5, "ساختارهای گسسته"),
        CurriculumCourseEntity("CS_COMP_NET_LAB", "COMP_ENG", "2115504", "آزمایشگاه شبکه", 1, "آزمایشگاهی", 5, coRequisites = "شبکه‌های کامپیوتری 1"),
        // ترم 6
        CurriculumCourseEntity("CS_SOFTWARE_ENG", "COMP_ENG", "2115601", "مهندسی نرم‌افزار 1", 3, "تخصصی", 6, "پایگاه داده‌ها"),
        CurriculumCourseEntity("CS_COMPILER", "COMP_ENG", "2115602", "اصول طراحی کامپایلر", 3, "تخصصی", 6, "نظریه زبان‌ها و اتوماتا"),
        CurriculumCourseEntity("CS_WEB_DEV", "COMP_ENG", "2115603", "توسعه برنامه‌های وب و موبایل", 3, "تخصصی", 6, "پایگاه داده‌ها"),
        // ترم 7
        CurriculumCourseEntity("CS_CYBER_SEC", "COMP_ENG", "2115701", "امنیت شبکه و اطلاعات", 3, "تخصصی", 7, "شبکه‌های کامپیوتری 1"),
        CurriculumCourseEntity("CS_CLOUD", "COMP_ENG", "2115702", "رایانش ابری و سامانه‌های توزیع‌شده", 3, "تخصصی", 7, "سیستم‌های عامل"),
        CurriculumCourseEntity("CS_INTERNSHIP", "COMP_ENG", "2115703", "کارآموزی مهندسی کامپیوتر", 2, "تخصصی", 7, "گذراندن 80 واحد"),
        // ترم 8
        CurriculumCourseEntity("CS_FINAL_PROJ", "COMP_ENG", "2115801", "پروژه پایانی کارشناسی کامپیوتر", 3, "تخصصی", 8, "مهندسی نرم‌افزار 1"),
        CurriculumCourseEntity("CS_DATA_MINING", "COMP_ENG", "2115802", "داده‌کاوی و کلان‌داده‌ها", 3, "تخصصی", 8, "هوش مصنوعی و یادگیری ماشین")
    )

    val civilEngineeringCourses: List<CurriculumCourseEntity> = listOf(
        // ترم 1
        CurriculumCourseEntity("CIV_MATH_1", "CIVIL_ENG", "3115101", "ریاضی عمومی 1", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CIV_PHYS_1", "CIVIL_ENG", "3115102", "فیزیک 1 (مکانیک)", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CIV_DRAW", "CIVIL_ENG", "3115103", "رسم فنی و نقشه‌کشی ساختمان", 2, "پایه", 1, ""),
        CurriculumCourseEntity("CIV_PROG", "CIVIL_ENG", "3115104", "برنامه‌نویسی و محاسبات کامپیوتری", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CIV_PERSIAN", "CIVIL_ENG", "3115105", "فارسی عمومی", 2, "عمومی", 1, ""),
        // ترم 2
        CurriculumCourseEntity("CIV_MATH_2", "CIVIL_ENG", "3115201", "ریاضی عمومی 2", 3, "پایه", 2, "ریاضی عمومی 1"),
        CurriculumCourseEntity("CIV_DIFF_EQ", "CIVIL_ENG", "3115202", "معادلات دیفرانسیل", 3, "پایه", 2, "ریاضی عمومی 1"),
        CurriculumCourseEntity("CIV_STATICS", "CIVIL_ENG", "3115203", "استاتیک", 3, "پایه", 2, "فیزیک 1"),
        CurriculumCourseEntity("CIV_GEOLOGY", "CIVIL_ENG", "3115204", "زمین‌شناسی مهندسی", 2, "تخصصی", 2, ""),
        // ترم 3
        CurriculumCourseEntity("CIV_STRENGTH_1", "CIVIL_ENG", "3115301", "مقاومت مصالح 1", 3, "تخصصی", 3, "استاتیک"),
        CurriculumCourseEntity("CIV_SURVEY_1", "CIVIL_ENG", "3115302", "نقشه‌برداری 1 و عملیات", 3, "تخصصی", 3, "ریاضی عمومی 1"),
        CurriculumCourseEntity("CIV_FLUID", "CIVIL_ENG", "3115303", "مکانیک سیالات عمران", 3, "تخصصی", 3, "استاتیک، ریاضی عمومی 2"),
        CurriculumCourseEntity("CIV_CONCRETE_TECH", "CIVIL_ENG", "3115304", "تکنولوژی و طرح اختلاط بتن", 2, "تخصصی", 3, "مصالح ساختمانی"),
        // ترم 4
        CurriculumCourseEntity("CIV_STRUCT_1", "CIVIL_ENG", "3115401", "تحلیل سازه‌ها 1", 3, "تخصصی", 4, "مقاومت مصالح 1"),
        CurriculumCourseEntity("CIV_SOIL", "CIVIL_ENG", "3115402", "مکانیک خاک", 3, "تخصصی", 4, "مقاومت مصالح 1"),
        CurriculumCourseEntity("CIV_HYDRAULIC", "CIVIL_ENG", "3115403", "هیدرولیک و آزمایشگاه", 3, "تخصصی", 4, "مکانیک سیالات"),
        CurriculumCourseEntity("CIV_STRENGTH_2", "CIVIL_ENG", "3115404", "مقاومت مصالح 2", 2, "تخصصی", 4, "مقاومت مصالح 1"),
        // ترم 5 تا 8
        CurriculumCourseEntity("CIV_STEEL_1", "CIVIL_ENG", "3115501", "طراحی سازه‌های فولادی 1", 3, "تخصصی", 5, "تحلیل سازه‌ها 1"),
        CurriculumCourseEntity("CIV_CONCRETE_1", "CIVIL_ENG", "3115502", "طراحی سازه‌های بتن‌آرمه 1", 3, "تخصصی", 5, "تحلیل سازه‌ها 1"),
        CurriculumCourseEntity("CIV_FOUNDATION", "CIVIL_ENG", "3115601", "مهندسی پی و پی‌سازی", 3, "تخصصی", 6, "مکانیک خاک"),
        CurriculumCourseEntity("CIV_EARTHQUAKE", "CIVIL_ENG", "3115701", "مهندسی زلزله و دینامیک سازه", 3, "تخصصی", 7, "تحلیل سازه‌ها 1"),
        CurriculumCourseEntity("CIV_PROJECT", "CIVIL_ENG", "3115801", "پروژه سازه‌های فولادی و بتنی", 3, "تخصصی", 8, "سازه‌های فولادی 1، بتن‌آرمه 1")
    )

    val appliedChemistryCourses: List<CurriculumCourseEntity> = listOf(
        // ترم 1
        CurriculumCourseEntity("CH_GEN_1", "CHEMISTRY", "4115101", "شیمی عمومی 1", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CH_GEN_LAB_1", "CHEMISTRY", "4115102", "آزمایشگاه شیمی عمومی 1", 1, "آزمایشگاهی", 1, coRequisites = "شیمی عمومی 1"),
        CurriculumCourseEntity("CH_MATH_1", "CHEMISTRY", "4115103", "ریاضی عمومی 1", 3, "پایه", 1, ""),
        CurriculumCourseEntity("CH_PHYS_1", "CHEMISTRY", "4115104", "فیزیک عمومی 1", 3, "پایه", 1, ""),
        // ترم 2
        CurriculumCourseEntity("CH_GEN_2", "CHEMISTRY", "4115201", "شیمی عمومی 2", 3, "پایه", 2, "شیمی عمومی 1"),
        CurriculumCourseEntity("CH_MATH_2", "CHEMISTRY", "4115202", "ریاضی عمومی 2", 3, "پایه", 2, "ریاضی عمومی 1"),
        CurriculumCourseEntity("CH_DIFF", "CHEMISTRY", "4115203", "معادلات دیفرانسیل", 3, "پایه", 2, "ریاضی عمومی 1"),
        // ترم 3
        CurriculumCourseEntity("CH_ORG_1", "CHEMISTRY", "4115301", "شیمی آلی 1", 3, "تخصصی", 3, "شیمی عمومی 2"),
        CurriculumCourseEntity("CH_ANAL_1", "CHEMISTRY", "4115302", "شیمی تجزیه 1", 3, "تخصصی", 3, "شیمی عمومی 2"),
        CurriculumCourseEntity("CH_PHYS_1_C", "CHEMISTRY", "4115303", "شیمی فیزیک 1", 3, "تخصصی", 3, "شیمی عمومی 2، ریاضی 2"),
        // ترم 4
        CurriculumCourseEntity("CH_ORG_2", "CHEMISTRY", "4115401", "شیمی آلی 2", 3, "تخصصی", 4, "شیمی آلی 1"),
        CurriculumCourseEntity("CH_ANAL_2", "CHEMISTRY", "4115402", "شیمی تجزیه 2 (دستگاهی)", 3, "تخصصی", 4, "شیمی تجزیه 1"),
        CurriculumCourseEntity("CH_INORG_1", "CHEMISTRY", "4115403", "شیمی معدنی 1", 3, "تخصصی", 4, "شیمی عمومی 2"),
        CurriculumCourseEntity("CH_IND_CALC", "CHEMISTRY", "4115404", "اصول محاسبات شیمی صنعتی", 3, "تخصصی", 4, "شیمی عمومی 2")
    )

    fun getCoursesForMajor(major: String): List<CurriculumCourseEntity> {
        val cleanMajor = major.trim()
        return when {
            cleanMajor.contains("شیمی کاربردی") || cleanMajor.contains("شیمی محض") || cleanMajor.contains("علوم شیمی") -> appliedChemistryCourses
            cleanMajor.contains("کامپیوتر") || cleanMajor.contains("نرم‌افزار") || cleanMajor.contains("IT") || cleanMajor.contains("هوش مصنوعی") -> computerEngineeringCourses
            cleanMajor.contains("عمران") || cleanMajor.contains("سازه‌") || cleanMajor.contains("نقشه‌برداری") -> civilEngineeringCourses
            cleanMajor.contains("شیمی") || cleanMajor.contains("پلیمر") || cleanMajor.contains("نفت") || cleanMajor.contains("فرآیند") -> chemicalEngineeringCourses
            else -> generateDynamicCurriculum(cleanMajor)
        }
    }

    /**
     * Fallback generator that produces dynamic, realistic 8-semester course charts
     * for any customized major name typed by the student (e.g. حقوق، روانشناسی، مدیریت، کشاورزی).
     */
    fun generateDynamicCurriculum(major: String): List<CurriculumCourseEntity> {
        val list = mutableListOf<CurriculumCourseEntity>()
        val baseCode = 1000 + (major.hashCode().coerceAtLeast(0) % 8000)

        // Term 1
        list.add(CurriculumCourseEntity("${major}_T1_1", major, "${baseCode}101", "مبانی و مقدمات $major", 3, "پایه", 1, ""))
        list.add(CurriculumCourseEntity("${major}_T1_2", major, "${baseCode}102", "اصول پایه و روش‌شناسی", 3, "پایه", 1, ""))
        list.add(CurriculumCourseEntity("${major}_T1_3", major, "${baseCode}103", "کاربرد کامپیوتر و آمار", 3, "پایه", 1, ""))
        list.add(CurriculumCourseEntity("${major}_T1_4", major, "${baseCode}104", "فارسی عمومی", 2, "عمومی", 1, ""))
        list.add(CurriculumCourseEntity("${major}_T1_5", major, "${baseCode}105", "زبان خارجی عمومی", 2, "عمومی", 1, ""))

        // Term 2
        list.add(CurriculumCourseEntity("${major}_T2_1", major, "${baseCode}201", "مفاهیم پیشرفته $major 1", 3, "پایه", 2, "مبانی و مقدمات $major"))
        list.add(CurriculumCourseEntity("${major}_T2_2", major, "${baseCode}202", "تحلیل ساختار و سیستم‌ها", 3, "تخصصی", 2, "اصول پایه و روش‌شناسی"))
        list.add(CurriculumCourseEntity("${major}_T2_3", major, "${baseCode}203", "روش تحقیق و گزارش‌نویسی", 2, "تخصصی", 2, ""))
        list.add(CurriculumCourseEntity("${major}_T2_4", major, "${baseCode}204", "اندیشه اسلامی 1", 2, "عمومی", 2, ""))

        // Term 3
        list.add(CurriculumCourseEntity("${major}_T3_1", major, "${baseCode}301", "دروس تخصصی و تحلیلی $major", 3, "تخصصی", 3, "مفاهیم پیشرفته $major 1"))
        list.add(CurriculumCourseEntity("${major}_T3_2", major, "${baseCode}302", "اصول طراحی و مدل‌سازی", 3, "تخصصی", 3, "تحلیل ساختار و سیستم‌ها"))
        list.add(CurriculumCourseEntity("${major}_T3_3", major, "${baseCode}303", "کارگاه تخصصی 1", 2, "تخصصی", 3, ""))
        list.add(CurriculumCourseEntity("${major}_T3_4", major, "${baseCode}304", "اخلاق اسلامی و حرفه‌ای", 2, "عمومی", 3, ""))

        // Term 4
        list.add(CurriculumCourseEntity("${major}_T4_1", major, "${baseCode}401", "مفاهیم پیشرفته $major 2", 3, "تخصصی", 4, "دروس تخصصی و تحلیلی $major"))
        list.add(CurriculumCourseEntity("${major}_T4_2", major, "${baseCode}402", "سیستم‌های جامع $major", 3, "تخصصی", 4, "اصول طراحی و مدل‌سازی"))
        list.add(CurriculumCourseEntity("${major}_T4_3", major, "${baseCode}403", "ارزیابی و بهینه‌سازی", 3, "تخصصی", 4, "دروس تخصصی و تحلیلی $major"))
        list.add(CurriculumCourseEntity("${major}_T4_4", major, "${baseCode}404", "اندیشه اسلامی 2", 2, "عمومی", 4, "اندیشه اسلامی 1"))

        // Term 5 to 8
        list.add(CurriculumCourseEntity("${major}_T5_1", major, "${baseCode}501", "مباحث ویژه و کاربردی 1", 3, "تخصصی", 5, "مفاهیم پیشرفته $major 2"))
        list.add(CurriculumCourseEntity("${major}_T6_1", major, "${baseCode}601", "مباحث ویژه و کاربردی 2", 3, "تخصصی", 6, "مباحث ویژه و کاربردی 1"))
        list.add(CurriculumCourseEntity("${major}_T7_1", major, "${baseCode}701", "کارآموزی و تمرین عملیاتی", 2, "تخصصی", 7, "گذراندن 80 واحد"))
        list.add(CurriculumCourseEntity("${major}_T8_1", major, "${baseCode}801", "پروژه کارشناسی و تحقیق پایانی", 3, "تخصصی", 8, "مباحث ویژه و کاربردی 2"))

        return list
    }
}

