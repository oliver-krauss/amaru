float table[512];

int calcLookupTablePosition(float val) {
    float stepSize,position;
    int tablePosition;

    float higher_end;
    higher_end = 2.0;
    float lower_end;
    lower_end = 0.5;
    int table_size;
    table_size = 512;

    stepSize = (higher_end - lower_end) / table_size;
    position = (val - lower_end) / stepSize;
    tablePosition = (int) position;

    if (tablePosition <= -1) {
        tablePosition = 0;
    }
    if (tablePosition >= table_size) {
        tablePosition = table_size - 1;
    }

    return tablePosition;
}

float sq_fn(float result) {
    return result * result;
}

float sq_der(float result) {
    return 2 * result;
}

float fn(float x) {
    float result, h;
    int i, tablePosition;
    tablePosition = calcLookupTablePosition(x);

    result = table[tablePosition];

    for (i = 0; i < 6; i = i + 1) {
        h = (sq_fn(result) - x) / sq_der(result);
        result = result - h;
    }

    return result;
}

float main() {
table[0] = 8.414455429209467540730E-01;
table[1] = 8.427381687957494760255E-01;
table[2] = 8.440269867853119611922E-01;
table[3] = 8.452228642282638704231E-01;
table[4] = 8.463166628830374182968E-01;
table[5] = 8.476555043320074256386E-01;
table[6] = 8.487229084604330786590E-01;
table[7] = 8.499150193852988977028E-01;
table[8] = 8.511311913947384732992E-01;
table[9] = 8.524341123070686698782E-01;
table[10] = 8.535865690468216193665E-01;
table[11] = 8.547632753106898517714E-01;
table[12] = 8.558424120369535481601E-01;
table[13] = 8.573710211370977329892E-01;
table[14] = 8.581695059710716666501E-01;
table[15] = 8.593288460736817624408E-01;
table[16] = 8.604776558281667098171E-01;
table[17] = 8.617127145392209364161E-01;
table[18] = 8.627667803671393853548E-01;
table[19] = 8.639948244954008815810E-01;
table[20] = 8.651433198149089953688E-01;
table[21] = 8.661706566321601963310E-01;
table[22] = 8.676413213302320848186E-01;
table[23] = 8.680761598142892054852E-01;
table[24] = 8.695340764384191789560E-01;
table[25] = 8.706188024479942777489E-01;
table[26] = 8.718658346123354663249E-01;
table[27] = 8.728534027917196347346E-01;
table[28] = 8.740670518299855640265E-01;
table[29] = 8.750196101347313959451E-01;
table[30] = 8.762260898540304987492E-01;
table[31] = 8.773365613260180051469E-01;
table[32] = 8.784196043995290414941E-01;
table[33] = 8.793648765863809613208E-01;
table[34] = 8.808054140253088748480E-01;
table[35] = 8.812400738851117187878E-01;
table[36] = 8.827032520985376162770E-01;
table[37] = 8.836632529101278166195E-01;
table[38] = 8.844665269477812730159E-01;
table[39] = 8.857632989109704979569E-01;
table[40] = 8.869444459825988058554E-01;
table[41] = 8.878604926220245996404E-01;
table[42] = 8.890170126066797573472E-01;
table[43] = 8.900802825639442250605E-01;
table[44] = 8.910025415315613317446E-01;
table[45] = 8.920092130206851432916E-01;
table[46] = 8.931448473419081324209E-01;
table[47] = 8.940939546601385234936E-01;
table[48] = 8.952229356645217128730E-01;
table[49] = 8.961350472996366667289E-01;
table[50] = 8.972562576875836581891E-01;
table[51] = 8.982748458111667888559E-01;
table[52] = 8.989087034813035836933E-01;
table[53] = 9.001779690738633821212E-01;
table[54] = 9.011825167469489361594E-01;
table[55] = 9.021814183963131617006E-01;
table[56] = 9.032751435461502387270E-01;
table[57] = 9.042775320757602264266E-01;
table[58] = 9.054917961809428872400E-01;
table[59] = 9.062579695322007244940E-01;
table[60] = 9.072150328758291637499E-01;
table[61] = 9.080948008752801614563E-01;
table[62] = 9.091770631451591144767E-01;
table[63] = 9.101523163226188373187E-01;
table[64] = 9.107874420412714888684E-01;
table[65] = 9.119599429670788293123E-01;
table[66] = 9.129252907878089295934E-01;
table[67] = 9.140293843573427112759E-01;
table[68] = 9.148584754059053691932E-01;
table[69] = 9.159262340730732754324E-01;
table[70] = 9.168829501662253411709E-01;
table[71] = 9.178292951063272697709E-01;
table[72] = 9.187839463620121049914E-01;
table[73] = 9.193647448805618571654E-01;
table[74] = 9.202939098036508891454E-01;
table[75] = 9.218154724261524712858E-01;
table[76] = 9.225398332564247461107E-01;
table[77] = 9.233511195475763155827E-01;
table[78] = 9.245884428724262527055E-01;
table[79] = 9.253303728750373879919E-01;
table[80] = 9.262578943247729013066E-01;
table[81] = 9.273520385814201238972E-01;
table[82] = 9.279429107482862759682E-01;
table[83] = 9.288750375695224992256E-01;
table[84] = 9.299146102896822529971E-01;
table[85] = 9.306961336186468924936E-01;
table[86] = 9.318916302037317800355E-01;
table[87] = 9.326405361728112852759E-01;
table[88] = 9.334024344411272489097E-01;
table[89] = 9.341344251519119179505E-01;
table[90] = 9.350240730528696087021E-01;
table[91] = 9.361071836098120435565E-01;
table[92] = 9.372797887968509211731E-01;
table[93] = 9.378691752391298264158E-01;
table[94] = 9.388998515717871251596E-01;
table[95] = 9.397780128821214828960E-01;
table[96] = 9.405242323131806081449E-01;
table[97] = 9.414138802141386319633E-01;
table[98] = 9.424025307206213009081E-01;
table[99] = 9.431534740595893806514E-01;
table[100] = 9.440199639231806605366E-01;
table[101] = 9.450198269011973462028E-01;
table[102] = 9.456072598991465127938E-01;
table[103] = 9.467601601960876633868E-01;
table[104] = 9.474804896856016123152E-01;
table[105] = 9.483515746273584179349E-01;
table[106] = 9.495314798379461684874E-01;
table[107] = 9.503392526515178628799E-01;
table[108] = 9.509079911853672850697E-01;
table[109] = 9.517575365667553244364E-01;
table[110] = 9.524077454695804023643E-01;
table[111] = 9.534617714462227944239E-01;
table[112] = 9.543074394269202009866E-01;
table[113] = 9.552802509464315416920E-01;
table[114] = 9.559872935844067054489E-01;
table[115] = 9.566713394872400355595E-01;
table[116] = 9.576465559193614307532E-01;
table[117] = 9.586152244304378955420E-01;
table[118] = 9.594465819692464814139E-01;
table[119] = 9.600067246294698986020E-01;
table[120] = 9.611093771997504253690E-01;
table[121] = 9.618056583476338738947E-01;
table[122] = 9.624407840662866364667E-01;
table[123] = 9.632640080541647886747E-01;
table[124] = 9.642454970344610520883E-01;
table[125] = 9.650623425374441621827E-01;
table[126] = 9.658816268587330222672E-01;
table[127] = 9.670072828934800357459E-01;
table[128] = 9.677941323019216834922E-01;
table[129] = 9.683127099885882049790E-01;
table[130] = 9.691081394174654306539E-01;
table[131] = 9.699290146157547587435E-01;
table[132] = 9.708430897736497477979E-01;
table[133] = 9.713964398550799517196E-01;
table[134] = 9.723313236115275204341E-01;
table[135] = 9.732478860179255564233E-01;
table[136] = 9.741987722380984759951E-01;
table[137] = 9.749628941721729669112E-01;
table[138] = 9.757522065940601452283E-01;
table[139] = 9.764218346868686060347E-01;
table[140] = 9.772034441686739958399E-01;
table[141] = 9.779904181984820921159E-01;
table[142] = 9.787673253515040139305E-01;
table[143] = 9.796941247315046341626E-01;
table[144] = 9.803122642329221703150E-01;
table[145] = 9.812486203941728213351E-01;
table[146] = 9.818594964971365524065E-01;
table[147] = 9.825181351417408004423E-01;
table[148] = 9.832835429794563042449E-01;
table[149] = 9.840484916030102224838E-01;
table[150] = 9.849590378987573835445E-01;
table[151] = 9.858619331419410380946E-01;
table[152] = 9.866066316632238208939E-01;
table[153] = 9.872506690829394582209E-01;
table[154] = 9.877598028512765138132E-01;
table[155] = 9.887733597963686005272E-01;
table[156] = 9.895332205305136241691E-01;
table[157] = 9.902893332815012605863E-01;
table[158] = 9.910159492007516446321E-01;
table[159] = 9.918973845455557603046E-01;
table[160] = 9.923981059131372628457E-01;
table[161] = 9.931355793807751242142E-01;
table[162] = 9.940398853448935145849E-01;
table[163] = 9.944781697279373444687E-01;
table[164] = 9.956267144878687691545E-01;
table[165] = 9.963803032434742634749E-01;
table[166] = 9.969913393192457728986E-01;
table[167] = 9.977359324277348395782E-01;
table[168] = 9.984752009051290899677E-01;
table[169] = 9.992165191650854705330E-01;
table[170] = 9.996061264105658983325E-01;
table[171] = 1.000555097902030121659E+00;
table[172] = 1.001432215622391153076E+00;
table[173] = 1.002163030012039213190E+00;
table[174] = 1.002628635788481270552E+00;
table[175] = 1.003364130383227026400E+00;
table[176] = 1.004195478736344515625E+00;
table[177] = 1.004880562440272795399E+00;
table[178] = 1.005774286173083620355E+00;
table[179] = 1.006497541280062968383E+00;
table[180] = 1.006977512689739118201E+00;
table[181] = 1.008000155007793274820E+00;
table[182] = 1.008396097041293515417E+00;
table[183] = 1.009190192286481213557E+00;
table[184] = 1.010202054845610764033E+00;
table[185] = 1.010493834451412054065E+00;
table[186] = 1.011490267280666044414E+00;
table[187] = 1.012168859811364507451E+00;
table[188] = 1.012907425051973842045E+00;
table[189] = 1.013602594147346103171E+00;
table[190] = 1.014314587602580974846E+00;
table[191] = 1.014989011556370224199E+00;
table[192] = 1.015538517990616451669E+00;
table[193] = 1.016473931705714983664E+00;
table[194] = 1.017096224969412210370E+00;
table[195] = 1.017793312927987470218E+00;
table[196] = 1.018497241034699740680E+00;
table[197] = 1.019246260821960348153E+00;
table[198] = 1.019649534850274541498E+00;
table[199] = 1.020399550953480671822E+00;
table[200] = 1.021260007909214673205E+00;
table[201] = 1.021934522604084127906E+00;
table[202] = 1.022623253136595966595E+00;
table[203] = 1.023307085159514073780E+00;
table[204] = 1.024000301976974780516E+00;
table[205] = 1.024452007673249731567E+00;
table[206] = 1.025409536145876598923E+00;
table[207] = 1.025862758155987375375E+00;
table[208] = 1.026550385822656119572E+00;
table[209] = 1.027439391646590260976E+00;
table[210] = 1.027892708582965441977E+00;
table[211] = 1.028509378394484752306E+00;
table[212] = 1.029393580656872364543E+00;
table[213] = 1.029861980398703247275E+00;
table[214] = 1.030804295360494116096E+00;
table[215] = 1.031254847669639262975E+00;
table[216] = 1.032083231604916884194E+00;
table[217] = 1.032586419679400124849E+00;
table[218] = 1.033457511538292239450E+00;
table[219] = 1.033850302347237182232E+00;
table[220] = 1.034794037210020389850E+00;
table[221] = 1.035445916972275393064E+00;
table[222] = 1.036053471325220076693E+00;
table[223] = 1.036544519940377329092E+00;
table[224] = 1.037157965217766930621E+00;
table[225] = 1.037876458429674464412E+00;
table[226] = 1.038688437094336336486E+00;
table[227] = 1.039162806093202551239E+00;
table[228] = 1.039984217797849330722E+00;
table[229] = 1.040641662270270328960E+00;
table[230] = 1.041031269515750867782E+00;
table[231] = 1.041775572660781579160E+00;
table[232] = 1.042571914894313556132E+00;
table[233] = 1.043008751560006386327E+00;
table[234] = 1.043722268317528456549E+00;
table[235] = 1.044367477690502532539E+00;
table[236] = 1.044980953360255426787E+00;
table[237] = 1.045651410307911088893E+00;
table[238] = 1.046229632484758642619E+00;
table[239] = 1.046924231746470868032E+00;
table[240] = 1.047555237230162417106E+00;
table[241] = 1.048340877388981740026E+00;
table[242] = 1.048818974239858370368E+00;
table[243] = 1.049668812441001408686E+00;
table[244] = 1.050301724098963829235E+00;
table[245] = 1.050890341478498246630E+00;
table[246] = 1.051518532775228775833E+00;
table[247] = 1.052151386948471767369E+00;
table[248] = 1.052607673348840755523E+00;
table[249] = 1.053414186841631572378E+00;
table[250] = 1.054011131645935517298E+00;
table[251] = 1.054647048987280344434E+00;
table[252] = 1.055081840476664201134E+00;
table[253] = 1.055908736261570446402E+00;
table[254] = 1.056518872895060523476E+00;
table[255] = 1.057186505332293968706E+00;
table[256] = 1.057417821565002524764E+00;
table[257] = 1.058218686636467786855E+00;
table[258] = 1.058996191031350564415E+00;
table[259] = 1.059623170451055473862E+00;
table[260] = 1.060238153066800359525E+00;
table[261] = 1.060684574532252177903E+00;
table[262] = 1.061457034437914614955E+00;
table[263] = 1.062057127737155548530E+00;
table[264] = 1.062671137427167122880E+00;
table[265] = 1.063302274667953239984E+00;
table[266] = 1.063706322423213279649E+00;
table[267] = 1.064549271204173530592E+00;
table[268] = 1.064919627493291276465E+00;
table[269] = 1.065719559168482266642E+00;
table[270] = 1.066336884432997678118E+00;
table[271] = 1.066775224827447132725E+00;
table[272] = 1.067540810459773670260E+00;
table[273] = 1.068133000226525020082E+00;
table[274] = 1.068727505095800944801E+00;
table[275] = 1.069338491513122546550E+00;
table[276] = 1.069922327107912130018E+00;
table[277] = 1.070363960006786241408E+00;
table[278] = 1.070936962757006671865E+00;
table[279] = 1.071728337864572555560E+00;
table[280] = 1.072305651695939943124E+00;
table[281] = 1.072751783090144295230E+00;
table[282] = 1.073533074524454677956E+00;
table[283] = 1.073939776596735118375E+00;
table[284] = 1.074676352571552362392E+00;
table[285] = 1.075304364950671720536E+00;
table[286] = 1.075895637161552675920E+00;
table[287] = 1.076298754109410449331E+00;
table[288] = 1.077028756853535718108E+00;
table[289] = 1.077460762875296795826E+00;
table[290] = 1.078059224182077224796E+00;
table[291] = 1.078602117615341526857E+00;
table[292] = 1.079365700786390380728E+00;
table[293] = 1.079765988445989366440E+00;
table[294] = 1.080407606910910534026E+00;
table[295] = 1.081132254314083995794E+00;
table[296] = 1.081714626859041628038E+00;
table[297] = 1.082294297727063536740E+00;
table[298] = 1.082705631618577024611E+00;
table[299] = 1.083443469806228742769E+00;
table[300] = 1.083991291395602241110E+00;
table[301] = 1.084385471984751436736E+00;
table[302] = 1.085173571455264163177E+00;
table[303] = 1.085556645588052093743E+00;
table[304] = 1.086112186362915732474E+00;
table[305] = 1.086892439594217218968E+00;
table[306] = 1.087429465992560961851E+00;
table[307] = 1.088016132454994844281E+00;
table[308] = 1.088405684129902573076E+00;
table[309] = 1.089168347692259564141E+00;
table[310] = 1.089719044268685932408E+00;
table[311] = 1.090289947795042291290E+00;
table[312] = 1.090669375891672343570E+00;
table[313] = 1.091235950620579986392E+00;
table[314] = 1.091806736062242988439E+00;
table[315] = 1.092388826815363866984E+00;
table[316] = 1.093111910095502237183E+00;
table[317] = 1.093672255595013576013E+00;
table[318] = 1.094221376657213307126E+00;
table[319] = 1.094798119779329281798E+00;
table[320] = 1.095357000635058364324E+00;
table[321] = 1.095728147288648646196E+00;
table[322] = 1.096280674969770885241E+00;
table[323] = 1.096820707536793282344E+00;
table[324] = 1.097404779064921509502E+00;
table[325] = 1.098122861455785637119E+00;
table[326] = 1.098667616806524582884E+00;
table[327] = 1.099214809155593330914E+00;
table[328] = 1.099774152814378957999E+00;
table[329] = 1.100345056340735316880E+00;
table[330] = 1.100871523960508024942E+00;
table[331] = 1.101418511718443848935E+00;
table[332] = 1.101961378458083729015E+00;
table[333] = 1.102501213859199635792E+00;
table[334] = 1.103070270937490438001E+00;
table[335] = 1.103588222227408932596E+00;
table[336] = 1.104140752004877379377E+00;
table[337] = 1.104530303679785330218E+00;
table[338] = 1.105217884976579245659E+00;
table[339] = 1.105596468603497806171E+00;
table[340] = 1.106326486696827426925E+00;
table[341] = 1.106860195241924804677E+00;
table[342] = 1.107392974878740554701E+00;
table[343] = 1.107917093047235734815E+00;
table[344] = 1.108471972720796561163E+00;
table[345] = 1.109009531449550278737E+00;
table[346] = 1.109380038322234307557E+00;
table[347] = 1.110087417365443140937E+00;
table[348] = 1.110464252254580452828E+00;
table[349] = 1.111152309193942366861E+00;
table[350] = 1.111517114095197822365E+00;
table[351] = 1.112208054869847861923E+00;
table[352] = 1.112743126915886504591E+00;
table[353] = 1.113282484743022937579E+00;
table[354] = 1.113659743873117280444E+00;
table[355] = 1.114333590929242445711E+00;
table[356] = 1.114856352267419525859E+00;
table[357] = 1.115413756035381176446E+00;
table[358] = 1.115939665623905341363E+00;
table[359] = 1.116464786123102737037E+00;
table[360] = 1.116987263612049474659E+00;
table[361] = 1.117488689686048752847E+00;
table[362] = 1.118016253054067377093E+00;
table[363] = 1.118389089903281785610E+00;
table[364] = 1.119070491368296504930E+00;
table[365] = 1.119603829744984624384E+00;
table[366] = 1.120126418499423071040E+00;
table[367] = 1.120628934833310719199E+00;
table[368] = 1.121153241006102030397E+00;
table[369] = 1.121668017775521608570E+00;
table[370] = 1.122204949415250618827E+00;
table[371] = 1.122722252436362255779E+00;
table[372] = 1.123229524945115187506E+00;
table[373] = 1.123764463930340129494E+00;
table[374] = 1.124253372168267706144E+00;
table[375] = 1.124782129759912763234E+00;
table[376] = 1.125270607118053600715E+00;
table[377] = 1.125804687362256251149E+00;
table[378] = 1.126170775161698722755E+00;
table[379] = 1.126834228557560324546E+00;
table[380] = 1.127184429335555515550E+00;
table[381] = 1.127717808712583869024E+00;
table[382] = 1.128224418062823586340E+00;
table[383] = 1.128883200788211693677E+00;
table[384] = 1.129384273844012520627E+00;
table[385] = 1.129731247480042943820E+00;
table[386] = 1.130388526655978331803E+00;
table[387] = 1.130745235409692295292E+00;
table[388] = 1.131251700053357511777E+00;
table[389] = 1.131904491386590549951E+00;
table[390] = 1.132409107560555128202E+00;
table[391] = 1.132929195552831558302E+00;
table[392] = 1.133416937702013349565E+00;
table[393] = 1.133913918263247389007E+00;
table[394] = 1.134432682369827416480E+00;
table[395] = 1.134918475938381021706E+00;
table[396] = 1.135435505967388847282E+00;
table[397] = 1.135934532394507590070E+00;
table[398] = 1.136432935560281576315E+00;
table[399] = 1.136927914893891111703E+00;
table[400] = 1.137423563873743548314E+00;
table[401] = 1.137920700003542728851E+00;
table[402] = 1.138412982522547745035E+00;
table[403] = 1.138773558337903279991E+00;
table[404] = 1.139402235387373174547E+00;
table[405] = 1.139897270041126731144E+00;
table[406] = 1.140388611528804441164E+00;
table[407] = 1.140893836869220967500E+00;
table[408] = 1.141403209363123227860E+00;
table[409] = 1.141872310949541668990E+00;
table[410] = 1.142353780468625901179E+00;
table[411] = 1.142740948147463964091E+00;
table[412] = 1.143352843363904414176E+00;
table[413] = 1.143843678873820435271E+00;
table[414] = 1.144336369116597307283E+00;
table[415] = 1.144823433112272192957E+00;
table[416] = 1.145296422001574887517E+00;
table[417] = 1.145789593069445944096E+00;
table[418] = 1.146276066372140300942E+00;
table[419] = 1.146757336699860019280E+00;
table[420] = 1.147245645164521210546E+00;
table[421] = 1.147730188562312569900E+00;
table[422] = 1.148236795879017169142E+00;
table[423] = 1.148715781253350387558E+00;
table[424] = 1.149181108520854754929E+00;
table[425] = 1.149526707298084593489E+00;
table[426] = 1.150027480367524779226E+00;
table[427] = 1.150636527107029349892E+00;
table[428] = 1.150990689098629005116E+00;
table[429] = 1.151586120048185613030E+00;
table[430] = 1.152056700123064603147E+00;
table[431] = 1.152546930896233279285E+00;
table[432] = 1.153027210022389725808E+00;
table[433] = 1.153506389570137402600E+00;
table[434] = 1.153853389329278611442E+00;
table[435] = 1.154462483900022373717E+00;
table[436] = 1.154928104813119471572E+00;
table[437] = 1.155262369681429479584E+00;
table[438] = 1.155752332959181449823E+00;
table[439] = 1.156221755084632762944E+00;
table[440] = 1.156700828773997269394E+00;
table[441] = 1.157177671338236679688E+00;
table[442] = 1.157650191244901094478E+00;
table[443] = 1.158260124081465836099E+00;
table[444] = 1.158595300330114818976E+00;
table[445] = 1.159186971643035857227E+00;
table[446] = 1.159665906752431530080E+00;
table[447] = 1.160144736303198476080E+00;
table[448] = 1.160446668965781080729E+00;
table[449] = 1.161069509925212406287E+00;
table[450] = 1.161534438386432777079E+00;
table[451] = 1.161852947134031532883E+00;
table[452] = 1.162469593792978095337E+00;
table[453] = 1.162934652684334935202E+00;
table[454] = 1.163260034760414951549E+00;
table[455] = 1.163878555247866986022E+00;
table[456] = 1.164327874449321509331E+00;
table[457] = 1.164779661001864585756E+00;
table[458] = 1.165252362744460867816E+00;
table[459] = 1.165715511000078974391E+00;
table[460] = 1.166015134800724606379E+00;
table[461] = 1.166516094483891929912E+00;
table[462] = 1.166986269168466705537E+00;
table[463] = 1.167557391676050881912E+00;
table[464] = 1.168004226624735686357E+00;
table[465] = 1.168348655342420938652E+00;
table[466] = 1.168817612605950406746E+00;
table[467] = 1.169270926850182856782E+00;
table[468] = 1.169858658053698707846E+00;
table[469] = 1.170311478855306175362E+00;
table[470] = 1.170634498256111433179E+00;
table[471] = 1.171107018162776070014E+00;
table[472] = 1.171679937478398381145E+00;
table[473] = 1.172011703010049643581E+00;
table[474] = 1.172458401740345657061E+00;
table[475] = 1.172914732082134925406E+00;
table[476] = 1.173381235556114621232E+00;
table[477] = 1.173836539764407271846E+00;
table[478] = 1.174279578211820052047E+00;
table[479] = 1.174743567007760836063E+00;
table[480] = 1.175301123947941839276E+00;
table[481] = 1.175751652414079195808E+00;
table[482] = 1.176201380384176875538E+00;
table[483] = 1.176534614025866565257E+00;
table[484] = 1.176990358224141219878E+00;
table[485] = 1.177551721369453519017E+00;
table[486] = 1.177999641608039826934E+00;
table[487] = 1.178442981199861350206E+00;
table[488] = 1.178895514905347718937E+00;
table[489] = 1.179330116371755332594E+00;
table[490] = 1.179775011904122816730E+00;
table[491] = 1.180221369809889342406E+00;
table[492] = 1.180561811786851267314E+00;
table[493] = 1.181131280907175229444E+00;
table[494] = 1.181572019909675974603E+00;
table[495] = 1.181896345668223657910E+00;
table[496] = 1.182446341494375108283E+00;
table[497] = 1.182794138341614287313E+00;
table[498] = 1.183334767420065603005E+00;
table[499] = 1.183779554251640941942E+00;
table[500] = 1.184081486914222880458E+00;
table[501] = 1.184668093005159850151E+00;
table[502] = 1.185097362689970568184E+00;
table[503] = 1.185535519835246187625E+00;
table[504] = 1.185990068112401596423E+00;
table[505] = 1.186423040769726711119E+00;
table[506] = 1.186755500928400541838E+00;
table[507] = 1.187170921669931766829E+00;
table[508] = 1.187723367404238672407E+00;
table[509] = 1.188163211607572655737E+00;
table[510] = 1.188603314583655645720E+00;
table[511] = 1.188940765184086645334E+00;

    int i;
    float sum, x;
    sum = 0.0;
    x = 0.5;
    for (i = 0; i < 1000; i = i + 1)  {
        sum = sum + fn(x) + fn(x+0.00015) + fn(x+0.0003) + fn(x+0.00045) + fn(x+0.0006) + fn(x+0.00075) + fn(x+0.0009) + fn(x+0.00105) + fn(x+0.0012) + fn(x+0.00135);
        x = x + 0.0015;
    }
    return sum;
}