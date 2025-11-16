package com.nyy.gmail.cloud.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class PhoneUtil {

    public static Phonenumber.PhoneNumber parsePhoneNumber(String number) {
        try {
            number = number.startsWith("+") ? number : "+" + number;
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber pn = phoneUtil.parse(number, null);
            return pn;
        } catch (NumberParseException e) {
            return null;
        }
    }

    public static int getCountryCodeForPhone(String phoneNumber) {
        Phonenumber.PhoneNumber number = parsePhoneNumber(phoneNumber);
        return number == null ? -1 : number.getCountryCode();
    }

    public static String getRegionCodeForPhone(String phoneNumber) {
        Phonenumber.PhoneNumber number = parsePhoneNumber(phoneNumber);
        return number == null ? null : PhoneNumberUtil.getInstance().getRegionCodeForNumber(number);
    }

}
