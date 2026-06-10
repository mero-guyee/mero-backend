package io.mero.app.global.enums;

import lombok.Getter;

@Getter
public enum Currency {
    // 주요 통화
    KRW("KRW", "대한민국 원", "₩"),
    USD("USD", "미국 달러", "$"),
    EUR("EUR", "유로", "€"),
    JPY("JPY", "일본 엔", "¥"),
    GBP("GBP", "영국 파운드", "£"),
    CNY("CNY", "중국 위안", "¥"),
    CHF("CHF", "스위스 프랑", "CHF"),
    CAD("CAD", "캐나다 달러", "C$"),
    AUD("AUD", "호주 달러", "A$"),

    // 남미 통화
    PEN("PEN", "페루 솔", "S/"),
    BOB("BOB", "볼리비아노", "Bs."),
    CLP("CLP", "칠레 페소", "$"),
    ARS("ARS", "아르헨티나 페소", "$"),
    BRL("BRL", "브라질 헤알", "R$"),
    COP("COP", "콜롬비아 페소", "$"),
    UYU("UYU", "우루과이 페소", "$U"),
    PYG("PYG", "파라과이 과라니", "₲"),
    VES("VES", "베네수엘라 볼리바르", "Bs.S"),
    GYD("GYD", "가이아나 달러", "G$"),
    SRD("SRD", "수리남 달러", "$"),

    // 아시아
    TWD("TWD", "대만 달러", "NT$"),
    HKD("HKD", "홍콩 달러", "HK$"),
    SGD("SGD", "싱가포르 달러", "S$"),
    THB("THB", "태국 바트", "฿"),
    VND("VND", "베트남 동", "₫"),
    PHP("PHP", "필리핀 페소", "₱"),
    MYR("MYR", "말레이시아 링깃", "RM"),
    IDR("IDR", "인도네시아 루피아", "Rp"),

    // 중동/아프리카
    AED("AED", "UAE 디르함", "د.إ"),
    SAR("SAR", "사우디 리얄", "﷼"),
    ZAR("ZAR", "남아공 랜드", "R"),
    EGP("EGP", "이집트 파운드", "£"),

    // 유럽
    SEK("SEK", "스웨덴 크로나", "kr"),
    NOK("NOK", "노르웨이 크로네", "kr"),
    DKK("DKK", "덴마크 크로네", "kr"),
    PLN("PLN", "폴란드 즐로티", "zł"),
    CZK("CZK", "체코 코루나", "Kč"),
    HUF("HUF", "헝가리 포린트", "Ft"),
    RUB("RUB", "러시아 루블", "₽"),

    // 기타
    NZD("NZD", "뉴질랜드 달러", "NZ$"),
    MXN("MXN", "멕시코 페소", "$"),
    INR("INR", "인도 루피", "₹");

    private final String code;
    private final String displayName;
    private final String symbol;

    Currency(String code, String displayName, String symbol) {
        this.code = code;
        this.displayName = displayName;
        this.symbol = symbol;
    }


}
