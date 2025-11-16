package com.nyy.gmail.cloud.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Socks5;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GoogleGenAiUtils {

    public static List<String> emailGeneratePromptList = List.of(
            "Please execute the following instruction (Task ID: req-$UUID).\n" +
                    "\n" +
                    "Below is the email body to be processed:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Requirements:\n" +
                    "1. Detect the primary language of the email body (ignore any HTML tags).\n" +
                    "2. Generate $N rewritten versions of the email content:\n" +
                    "    i. Polish and expand the original text by adding approximately 200 additional words, while keeping the original purpose and primary language unchanged.\n" +
                    "    ii. **Do not modify any HTML tags**, structure, or embedded image elements, If there are links in the copy, the part of the link should be moved to the end of the text to ensure that the link appears at the very end of the copy, without changing the purpose of the text.\n" +
                    "    iii. **Extract and retain the original marketing purpose and key information of the copywriting**, make modifications in other parts by more than 50%, and expand and polish to add at least 200 words.\n" +
                    "    iv. Do NOT use the following high-risk words or phrases**. If any of them appear in the source, replace them with appropriate synonyms or safer equivalents: \n" + " Daily Salary, Earn Daily, Limited spots, secure your spot, spots are limited, Apply here, limited, act now, apply immediately, urgent, Verify your account, Security alert, Account suspended, Password expired, SSN, Credit card number, Password, Porn, XXX, Adult, Sex, Casino, Bet, Gambling, Free (FREE), Free gift, No cost, Miracle cure, Instant results, Work from home, Earn $500/day, Make money fast, Winner, Bonus, Buy now, Order now, Click here, Exclusive invitation, VIP access, Urgent, Limited time, 100% satisfaction, Guaranteed.\n" +
                    "    v. Revise the copy using a personified, narrative (humanized storytelling) tone.\n" +
                    "    vi. Do not change the primary language used in the email body.\n" +
                    "    vii. All original placeholders (e.g., {{var1}}) must be kept intact.\n" +
                    "    viii. Assign a score (out of 10) to reflect the quality of each rewritten version.\n" +
                    "3. Return a **strictly formatted JSON array**. Each array item must contain exactly **three fields**:\n" +
                    "   - `lang`: the primary language of the email,\n" +
                    "   - `email`: the rewritten email body,\n" +
                    "   - `score`: the score for this rewrite (max: 10).\n" +
                    "\n" +
                    "**Do not include any additional explanations or text. Only return the JSON array.**\n",

            "Please execute the following instruction (Task ID: req-$UUID).\n" +
                    "\n" +
                    "Below is the email body to be processed:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Requirements:\n" +
                    "1. Detect the primary language of the email body (ignore any HTML tags).\n" +
                    "2. Generate $N rewritten versions of the email content:\n" +
                    "    i. Polish and expand the original text by adding approximately 200 additional words, while keeping the original purpose and primary language unchanged.\n" +
                    "    ii. **Do not modify any HTML tags**, structure, or embedded image elements, If there are links in the copy, the part of the link should be moved to the end of the text to ensure that the link appears at the very end of the copy, without changing the purpose of the text.\n" +
                    "    iii. **Extract and retain the original marketing purpose and key information of the copywriting**, make modifications in other parts by more than 50%, and expand and polish to add at least 200 words.\n" +
                    "    iv. Do NOT use the following high-risk words or phrases**. If any of them appear in the source, replace them with appropriate synonyms or safer equivalents: \n" + " Daily Salary, Earn Daily, Limited spots, secure your spot, spots are limited, Apply here, limited, act now, apply immediately, urgent, Verify your account, Security alert, Account suspended, Password expired, SSN, Credit card number, Password, Porn, XXX, Adult, Sex, Casino, Bet, Gambling, Free (FREE), Free gift, No cost, Miracle cure, Instant results, Work from home, Earn $500/day, Make money fast, Winner, Bonus, Buy now, Order now, Click here, Exclusive invitation, VIP access, Urgent, Limited time, 100% satisfaction, Guaranteed.\n" +
                    "    v. Revise the copy using a **scenario-based descriptive tone**.\n" +
                    "    vi. Do not change the primary language used in the email body.\n" +
                    "    vii. All original placeholders (e.g., {{var1}}) must be kept intact.\n" +
                    "    viii. Assign a score (out of 10) to reflect the quality of each rewritten version.\n" +
                    "3. Return a **strictly formatted JSON array**. Each array item must contain exactly **three fields**:\n" +
                    "   - `lang`: the primary language of the email,\n" +
                    "   - `email`: the rewritten email body,\n" +
                    "   - `score`: the score for this rewrite (max: 10).\n" +
                    "\n" +
                    "**Do not include any additional explanations or text. Only return the JSON array.**\n",

            "Please execute the following instruction (Task ID: req-$UUID).\n" +
                    "\n" +
                    "Below is the email body to be processed:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Requirements:\n" +
                    "1. Detect the primary language of the email body (ignore any HTML tags).\n" +
                    "2. Generate $N rewritten versions of the email content:\n" +
                    "    i. Polish and expand the original text by adding approximately 200 additional words, while keeping the original purpose and primary language unchanged.\n" +
                    "    ii. **Do not modify any HTML tags**, structure, or embedded image elements, If there are links in the copy, the part of the link should be moved to the end of the text to ensure that the link appears at the very end of the copy, without changing the purpose of the text.\n" +
                    "    iii. **Extract and retain the original marketing purpose and key information of the copywriting**, make modifications in other parts by more than 50%, and expand and polish to add at least 200 words.\n" +
                    "    iv. Do NOT use the following high-risk words or phrases**. If any of them appear in the source, replace them with appropriate synonyms or safer equivalents: \n" + " Daily Salary, Earn Daily, Limited spots, secure your spot, spots are limited, Apply here, limited, act now, apply immediately, urgent, Verify your account, Security alert, Account suspended, Password expired, SSN, Credit card number, Password, Porn, XXX, Adult, Sex, Casino, Bet, Gambling, Free (FREE), Free gift, No cost, Miracle cure, Instant results, Work from home, Earn $500/day, Make money fast, Winner, Bonus, Buy now, Order now, Click here, Exclusive invitation, VIP access, Urgent, Limited time, 100% satisfaction, Guaranteed.\n" +
                    "    v. Revise the copy using a **problem–solution oriented tone**.\n" +
                    "    vi. Do not change the primary language used in the email body.\n" +
                    "    vii. All original placeholders (e.g., {{var1}}) must be kept intact.\n" +
                    "    viii. Assign a score (out of 10) to reflect the quality of each rewritten version.\n" +
                    "3. Return a **strictly formatted JSON array**. Each array item must contain exactly **three fields**:\n" +
                    "   - `lang`: the primary language of the email,\n" +
                    "   - `email`: the rewritten email body,\n" +
                    "   - `score`: the score for this rewrite (max: 10).\n" +
                    "\n" +
                    "**Do not include any additional explanations or text. Only return the JSON array.**\n",

            "Please execute the following instruction (Task ID: req-$UUID).\n" +
                    "\n" +
                    "Below is the email body to be processed:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Requirements:\n" +
                    "1. Detect the primary language of the email body (ignore any HTML tags).\n" +
                    "2. Generate $N rewritten versions of the email content:\n" +
                    "    i. Polish and expand the original text by adding approximately 200 additional words, while keeping the original purpose and primary language unchanged.\n" +
                    "    ii. **Do not modify any HTML tags**, structure, or embedded image elements, If there are links in the copy, the part of the link should be moved to the end of the text to ensure that the link appears at the very end of the copy, without changing the purpose of the text.\n" +
                    "    iii. **Extract and retain the original marketing purpose and key information of the copywriting**, make modifications in other parts by more than 50%, and expand and polish to add at least 200 words.\n" +
                    "    iv. Do NOT use the following high-risk words or phrases**. If any of them appear in the source, replace them with appropriate synonyms or safer equivalents: \n" + " Daily Salary, Earn Daily, Limited spots, secure your spot, spots are limited, Apply here, limited, act now, apply immediately, urgent, Verify your account, Security alert, Account suspended, Password expired, SSN, Credit card number, Password, Porn, XXX, Adult, Sex, Casino, Bet, Gambling, Free (FREE), Free gift, No cost, Miracle cure, Instant results, Work from home, Earn $500/day, Make money fast, Winner, Bonus, Buy now, Order now, Click here, Exclusive invitation, VIP access, Urgent, Limited time, 100% satisfaction, Guaranteed.\n" +
                    "    v. Revise the copy using a **single-selling-point focused tone**.\n" +
                    "    vi. Do not change the primary language used in the email body.\n" +
                    "    vii. All original placeholders (e.g., {{var1}}) must be kept intact.\n" +
                    "    viii. Assign a score (out of 10) to reflect the quality of each rewritten version.\n" +
                    "3. Return a **strictly formatted JSON array**. Each array item must contain exactly **three fields**:\n" +
                    "   - `lang`: the primary language of the email,\n" +
                    "   - `email`: the rewritten email body,\n" +
                    "   - `score`: the score for this rewrite (max: 10).\n" +
                    "\n" +
                    "**Do not include any additional explanations or text. Only return the JSON array.**\n",

            "Please execute the following instruction (Task ID: req-$UUID).\n" +
                    "\n" +
                    "Below is the email body to be processed:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Requirements:\n" +
                    "1. Detect the primary language of the email body (ignore any HTML tags).\n" +
                    "2. Generate $N rewritten versions of the email content:\n" +
                    "    i. Polish and expand the original text by adding approximately 200 additional words, while keeping the original purpose and primary language unchanged.\n" +
                    "    ii. **Do not modify any HTML tags**, structure, or embedded image elements, If there are links in the copy, the part of the link should be moved to the end of the text to ensure that the link appears at the very end of the copy, without changing the purpose of the text.\n" +
                    "    iii. **Extract and retain the original marketing purpose and key information of the copywriting**, make modifications in other parts by more than 50%, and expand and polish to add at least 200 words.\n" +
                    "    iv. Do NOT use the following high-risk words or phrases**. If any of them appear in the source, replace them with appropriate synonyms or safer equivalents: \n" + " Daily Salary, Earn Daily, Limited spots, secure your spot, spots are limited, Apply here, limited, act now, apply immediately, urgent, Verify your account, Security alert, Account suspended, Password expired, SSN, Credit card number, Password, Porn, XXX, Adult, Sex, Casino, Bet, Gambling, Free (FREE), Free gift, No cost, Miracle cure, Instant results, Work from home, Earn $500/day, Make money fast, Winner, Bonus, Buy now, Order now, Click here, Exclusive invitation, VIP access, Urgent, Limited time, 100% satisfaction, Guaranteed.\n" +
                    "    v. Revise the copy using a **data-driven, customer-benefit emphasized tone**.\n" +
                    "    vi. Do not change the primary language used in the email body.\n" +
                    "    vii. All original placeholders (e.g., {{var1}}) must be kept intact.\n" +
                    "    viii. Assign a score (out of 10) to reflect the quality of each rewritten version.\n" +
                    "3. Return a **strictly formatted JSON array**. Each array item must contain exactly **three fields**:\n" +
                    "   - `lang`: the primary language of the email,\n" +
                    "   - `email`: the rewritten email body,\n" +
                    "   - `score`: the score for this rewrite (max: 10).\n" +
                    "\n" +
                    "**Do not include any additional explanations or text. Only return the JSON array.**\n"
    );

    // 邮件优化提示词
    public static List<String> emailOptimizePromptList = List.of(
            "请执行以下指令（任务编号：req-$UUID）。\n" +
                    "以下是需要处理的邮件正文：\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "要求：\n" +
                    "1. 识别邮件正文的主体语言（不考虑 HTML 标签内容）\n" +
                    "2. 要求修改$N个版本：" +
                        "i. 在不改变语义和邮件语言的前提下，对正文中的纯文本内容进行轻微措辞调整，以不同表达方式呈现，提升表达多样性，降低重复内容识别风险\n" +
                        "ii. 禁止更改任何 HTML 标签、结构或嵌入的图片元素\n" +
                        "iii. 每段文字最多允许调整 2 处用词或语序，避免大幅改动\n" +
                        "iv. 调整后的内容必须保留原有的语气、营销目的与语言风格\n" +
                        "v. 严禁大段增加或删除文字内容\n" +
                        "vi. 严禁修改邮件正文使用的主体语言\n" +
                        "vii. 要求原本的占位符不变, 例如：{{var1}}\n" +
                        "viii. 并对本次修改进行打分，最高10分\n" +
                    "3. 只返回严格格式的 JSON 数组, 数组中的对象包括三个字段：lang(邮件正文的主体语言)、email(修改后的邮件正文)、score(对本次修改进行打分数)，不得包含任何额外文字或解释。",

            "Please execute the following instruction (Task ID: req-$UUID).\n" +
                    "\n" +
                    "Below is the email body to be processed:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Requirements:\n" +
                    "1. Detect the primary language of the email body (ignore any HTML tags).\n" +
                    "2. Generate **$N rewritten versions** of the email content:\n" +
                    "    i. Slightly rephrase the plain text content using alternative expressions **without changing the original meaning or language**, in order to increase expression diversity and reduce the risk of duplicate content detection.\n" +
                    "    ii. **Do not modify any HTML tags**, structure, or embedded image elements.\n" +
                    "    iii. Make **no more than 2 small wording or word order changes per paragraph**; avoid major rewrites.\n" +
                    "    iv. The rewritten content must preserve the **original tone, marketing intent, and language style**.\n" +
                    "    v. **Do not add or remove large portions** of the original content.\n" +
                    "    vi. **Do not change** the primary language used in the email body.\n" +
                    "    vii. All original **placeholders (e.g., {{var1}})** must be kept intact.\n" +
                    "    viii. Assign a **score (out of 10)** to reflect the quality of each rewritten version.\n" +
                    "3. Return a **strictly formatted JSON array**. Each array item must contain exactly **three fields**:\n" +
                    "   - `lang`: the primary language of the email,\n" +
                    "   - `email`: the rewritten email body,\n" +
                    "   - `score`: the score for this rewrite (max: 10).\n" +
                    "\n" +
                    "**Do not include any additional explanations or text. Only return the JSON array.**\n",

            "Execute the following task (Task ID: req-$UUID).\n" +
                    "\n" +
                    "Provided below is the email content to be processed:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Instructions:\n" +
                    "1. Identify the main language of the email body (exclude any HTML tags from detection).\n" +
                    "2. Generate $N variant versions of the content:\n" +
                    "   i. Lightly rephrase the plain text while maintaining its original meaning and language, using alternative wording to increase variation and reduce duplication detection risk.\n" +
                    "   ii. Do not alter any HTML tags, structure, or embedded image elements.\n" +
                    "   iii. Limit changes to no more than 2 wordings or word order adjustments per paragraph.\n" +
                    "   iv. Preserve the original tone, marketing goal, and language style.\n" +
                    "   v. Do not insert or delete large sections of text.\n" +
                    "   vi. The main language must remain unchanged.\n" +
                    "   vii. All placeholders (e.g., {{var1}}) must be left untouched.\n" +
                    "   viii. Each version must include a quality score out of 10.\n" +
                    "\n" +
                    "3. Return only a strictly formatted JSON array, where each object includes:\n" +
                    "   - `lang`: the detected primary language,\n" +
                    "   - `email`: the rewritten email body,\n" +
                    "   - `score`: a numeric quality score (max 10).\n" +
                    "\n" +
                    "No other text or explanation should be returned.",

            "Task (ID: req-$UUID):\n" +
                    "\n" +
                    "Here’s the email content to process:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Steps:\n" +
                    "1. Detect the email’s primary language, ignoring any HTML tags.\n" +
                    "2. Rewrite the plain text part into $N alternative versions:\n" +
                    "   - Keep the language and meaning the same.\n" +
                    "   - Only make light edits (max 2 changes per paragraph).\n" +
                    "   - Do NOT touch HTML tags, structure, or images.\n" +
                    "   - Preserve tone, style, and purpose.\n" +
                    "   - Do NOT add or remove large text blocks.\n" +
                    "   - Keep all placeholders like {{var1}} unchanged.\n" +
                    "   - Rate each version (0–10).\n" +
                    "\n" +
                    "3. Output only a valid JSON array of objects with:\n" +
                    "   - `lang`\n" +
                    "   - `email`\n" +
                    "   - `score`\n" +
                    "\n" +
                    "No extra explanations. JSON only.",

            "You are assigned to process an email (Task ID: req-$UUID).\n" +
                    "\n" +
                    "Email content:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Your task:\n" +
                    "1. Identify the dominant language used in the text (ignore HTML tags).\n" +
                    "2. Create $N lightly rewritten versions:\n" +
                    "   - Rephrase only the plain text content without changing its meaning or language.\n" +
                    "   - HTML tags and structure must remain untouched.\n" +
                    "   - No more than two edits per paragraph (wording or order).\n" +
                    "   - Keep tone, intention, and writing style consistent.\n" +
                    "   - Do not insert or remove significant portions.\n" +
                    "   - Maintain placeholders like {{var1}} as-is.\n" +
                    "   - Score each version on a 10-point scale.\n" +
                    "\n" +
                    "3. Return a clean JSON array. Each item must have:\n" +
                    "   - `lang`\n" +
                    "   - `email`\n" +
                    "   - `score`\n" +
                    "\n" +
                    "No extra output—JSON only.",

            "Task Request: req-$UUID\n" +
                    "\n" +
                    "Target email body:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Requirements:\n" +
                    "1. Detect the primary language used (disregard any HTML tags).\n" +
                    "2. Generate $N lightly reworded versions of the plain text:\n" +
                    "   - The original meaning, language, tone, and marketing intent must be preserved.\n" +
                    "   - Do not modify HTML tags, structure, or embedded images.\n" +
                    "   - Each paragraph may only have up to two small wording or sequencing changes.\n" +
                    "   - No significant additions or deletions are allowed.\n" +
                    "   - Placeholders such as {{var1}} must remain unchanged.\n" +
                    "   - Assign a quality score (max: 10) to each version.\n" +
                    "\n" +
                    "3. Return only a JSON array, formatted as:\n" +
                    "```json\n" +
                    "[\n" +
                    "  {\"lang\": \"detected_language\", \"email\": \"rewritten_text\", \"score\": 9},\n" +
                    "  ...\n" +
                    "]" +
                    "```",
            "Task ID: req-$UUID\n" +
                    "\n" +
                    "Input:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Instructions:\n" +
                    "1. Detect primary language (ignore HTML).\n" +
                    "2. Rewrite $N versions:\n" +
                    "   - Preserve meaning, language, and tone.\n" +
                    "   - HTML tags/structure/images: untouched.\n" +
                    "   - Max 2 light edits per paragraph.\n" +
                    "   - No large additions/deletions.\n" +
                    "   - Keep placeholders (e.g., {{var1}}).\n" +
                    "   - Score each version (0–10).\n" +
                    "\n" +
                    "3. Output:\n" +
                    "JSON array of objects:\n" +
                    "```json\n" +
                    "[\n" +
                    "  { \"lang\": \"...\", \"email\": \"...\", \"score\": ... },\n" +
                    "  ...\n" +
                    "]" +
                    "```"
            );

    public static List<String> emailOptimizeTitlePromptList = List.of(
            "请执行以下指令（任务编号：req-$UUID）。\n" +
                    "以下是需要处理的邮件标题：\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "要求：\n" +
                    "1. 识别邮件标题的主体语言\n" +
                    "2. 要求修改$N个版本：" +
                    "i. 在不改变语义和邮件语言的前提下，对标题内容进行轻微措辞调整，以不同表达方式呈现，提升表达多样性，降低重复内容识别风险\n" +
                    "ii. 调整后的内容必须保留原有的语气、营销目的与语言风格\n" +
                    "iii. 严禁修改邮件标题使用的主体语言\n" +
                    "iv. 要求原本的占位符不变, 例如：{{var1}}\n" +
                    "v. 并对本次修改进行打分，最高10分\n" +
                    "3. 只返回严格格式的 JSON 数组, 数组中的对象包括三个字段：lang(邮件标题的主体语言)、title(修改后的邮件标题)、score(对本次修改进行打分数)，不得包含任何额外文字或解释。",

            "Please execute the following instructions (Task ID: req-$UUID).\n" +
                    "The following is the email subject to be processed:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "Requirements:\n" +
                    "1. Identify the primary language of the email subject.\n" +
                    "2. Generate $N modified versions:\n" +
                    "   i. Without changing the meaning or the original language of the subject, make slight wording adjustments to present the content in different expressions, enhancing diversity and reducing duplicate content detection risk.\n" +
                    "   ii. The modified content must retain the original tone, marketing intent, and language style.\n" +
                    "   iii. It is strictly forbidden to change the primary language used in the email subject.\n" +
                    "   iv. Placeholders must remain unchanged, e.g., {{var1}}.\n" +
                    "   v. Assign a score to each modification, with a maximum of 10.\n" +
                    "3. Only return a strictly formatted JSON array. Each object in the array must include three fields: lang (the primary language of the email subject), title (the modified email subject), score (the score for this modification). No additional text or explanation is allowed.",

            "Execute the following task (ID: req-$UUID).\n" +
                    "Here is the email subject to process:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "Instructions:\n" +
                    "1. Detect the main language of the subject line.\n" +
                    "2. Create $N variations:\n" +
                    "   i. Slightly rephrase the subject line without changing its meaning or language, providing alternative expressions to improve diversity and reduce repetition risk.\n" +
                    "   ii. Keep the original tone, marketing intent, and style intact.\n" +
                    "   iii. Do not alter the subject line’s primary language.\n" +
                    "   iv. Preserve placeholders (e.g., {{var1}}).\n" +
                    "   v. Rate each modification with a score up to 10.\n" +
                    "3. Return only a strict JSON array. Each object must contain: lang (subject’s main language), title (the revised subject), score (the score of this modification). No extra comments or explanations.",

            "Please follow the task below (Task No.: req-$UUID).\n" +
                    "The email subject line for processing is:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "Requirements:\n" +
                    "1. Identify the dominant language of the subject.\n" +
                    "2. Produce $N alternative versions:\n" +
                    "   i. Make light wording changes to the subject while keeping its meaning and language unchanged, ensuring variation to avoid duplication issues.\n" +
                    "   ii. Maintain the original tone, promotional purpose, and stylistic approach.\n" +
                    "   iii. Do not switch the subject’s language.\n" +
                    "   iv. Keep placeholders untouched, such as {{var1}}.\n" +
                    "   v. Score each version from 1–10.\n" +
                    "3. Output only a JSON array strictly formatted with objects including: lang, title, score. No additional explanation or text.",

            "Task execution required (Reference: req-$UUID).\n" +
                    "Process the following email subject line:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "Guidelines:\n" +
                    "1. Determine the subject’s primary language.\n" +
                    "2. Deliver $N revised versions:\n" +
                    "   i. Reword the subject slightly without altering its meaning or the original language, offering multiple expressions to enhance uniqueness.\n" +
                    "   ii. Retain the same tone, marketing goal, and stylistic character.\n" +
                    "   iii. The subject’s language must remain unchanged.\n" +
                    "   iv. Placeholders like {{var1}} must not be modified.\n" +
                    "   v. Rate each version with a score up to 10 points.\n" +
                    "3. Respond only with a JSON array. Each item must include: lang, title, score. No additional output or explanation.",

            "Carry out the following task (req-$UUID).\n" +
                    "Target email subject:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "Specifications:\n" +
                    "1. Identify the main language used in the subject.\n" +
                    "2. Generate $N different versions:\n" +
                    "   i. Apply slight rephrasing while keeping meaning and language intact, producing diverse expressions.\n" +
                    "   ii. Ensure the original style, tone, and marketing purpose are preserved.\n" +
                    "   iii. Do not alter the detected language.\n" +
                    "   iv. Placeholders such as {{var1}} must remain as is.\n" +
                    "   v. Give each variation a score (max: 10).\n" +
                    "3. Output only a JSON array. Each object must contain: lang, title, score. No extra commentary.",

            "Perform this instruction set (ID: req-$UUID).\n" +
                    "The subject line to handle is:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "Conditions:\n" +
                    "1. Detect the email subject’s core language.\n" +
                    "2. Provide $N alternative subject lines:\n" +
                    "   i. Adjust wording slightly without changing the meaning or language, to present alternative expressions and reduce repetition risk.\n" +
                    "   ii. Keep tone, intent, and style consistent with the original.\n" +
                    "   iii. The subject’s language must not be changed.\n" +
                    "   iv. Leave placeholders unchanged (e.g., {{var1}}).\n" +
                    "   v. Assign a rating (0–10) to each modification.\n" +
                    "3. Only return a strictly formatted JSON array with fields: lang, title, score. Do not include any other text or notes."
    );

    public static List<String> emailGenerateTitlePromptListV3 = List.of(
            // 中文版
            "你是一名专业的海外邮件标题优化专家。\n" +
                    "请执行以下指令（任务编号：req-$UUID）。\n" +
                    "\n" +
                    "【风格参数设置】\n" +
                    "情感基调: $emotion\n" +
                    "功能目标: $target\n" +
                    "发件角色: $character\n" +
                    "语言风格: $style\n" +
                    "其他特征: $other\n" +
                    "其他信息录入: $message\n" +
                    "（若某项为“默认”，则忽略该维度的风格干预）\n" +
                    "\n" +
                    "以下是需要处理的邮件内容：\n" +
                    "【邮件标题】:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "任务目标：\n" +
                    "1. 识别邮件的主体语言（lang）。\n" +
                    "2. 满足以下要求：\n" +
                    "   i. 在不改变语义、语言与事实内容的前提下，对标题与正文进行措辞调整，以不同表达方式呈现，主要调整文案的结构和文风，加强每个版本之间的差异性，差异度至少为50%，提升多样性并降低重复检测风险，降低邮件文案的垃圾率，提高邮件的 inbox 率。\n" +
                    "   ii. 保持文案的核心内容及相关信息、营销目的、品牌调性。\n" +
                    "   iii. 严禁修改邮件使用的主体语言。\n" +
                    "   iv. 保留原文中的占位符（如 {{var1}}、优惠码、链接等）。\n" +
                    "   v. 为每个修改版本进行打分（1–10），表示优化质量或自然度。低于7分的文案进行重新优化。\n" +
                    "\n" +
                    "风格与风控要求：\n" +
                    "1. 禁止使用夸张、诱导性、制造紧迫感的词汇。\n" +
                    "2. 不使用感叹号、表情符号或营销感过强的短语。\n" +
                    "3. 语言应自然、符合邮件规范，降低垃圾邮件识别风险，提高邮件 inbox 率。\n" +
                    "4. 若原始邮件为“文本+链接”结构，保留链接结构，仅优化文字部分。\n" +
                    "5. 禁止使用任何敏感词和敏感内容，全文少于三个表情符号。\n" +
                    "\n" +
                    "输出格式要求：\n" +
                    "仅返回严格格式的 JSON 数组，每个对象包含以下字段：\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"lang\": \"邮件主体语言\",\n" +
                    "    \"title\": \"优化后的邮件标题\",\n" +
                    "    \"score\": 8.7\n" +
                    "  }\n" +
                    "]\n" +
                    "不得包含任何解释、注释或额外文字。",
            // 英文版
            "You are a professional email copywriting and subject line optimization expert specializing in international email marketing.\n" +
                    "Please execute the following instructions (Task ID: req-$UUID).\n" +
                    "\n" +
                    "[Style Configuration]\n" +
                    "Emotional Tone: $emotion\n" +
                    "Functional Goal: $target\n" +
                    "Sender Role: $character\n" +
                    "Language Style: $style\n" +
                    "Additional Traits: $other\n" +
                    "Additional Information: $message\n" +
                    "(If any field is set to “default,” ignore that dimension in stylistic adjustments.)\n" +
                    "\n" +
                    "The following email content needs optimization:\n" +
                    "[Email Subject]:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "Task Objectives:\n" +
                    "1. Detect the primary language of the email (lang).\n" +
                    "2. following the rules below:\n" +
                    "   i. Without changing meaning, language, or factual content, rephrase the subject and body using different expressions and structures to strengthen stylistic variety. Ensure at least 50% variation across versions to enhance diversity, reduce duplication detection, lower spam risk, and improve inbox placement rate.\n" +
                    "   ii. Preserve the email’s core information, marketing intent, and brand tone.\n" +
                    "   iii. Do not change the main language of the email.\n" +
                    "   iv. Keep placeholders (e.g., {{var1}}, promo codes, URLs) exactly as in the original.\n" +
                    "   v. Assign each version a score (1–10) indicating optimization quality and naturalness. Re-optimize any version scoring below 7.\n" +
                    "\n" +
                    "Style and Compliance Guidelines:\n" +
                    "1. Avoid exaggerated, manipulative, or urgency-driven wording.\n" +
                    "2. Do not use exclamation marks, emojis, or overly promotional phrases.\n" +
                    "3. The tone must remain natural and professional to reduce spam detection risk and improve inbox deliverability.\n" +
                    "4. If the original email uses a 'text + link' format, preserve the structure and only optimize the textual part.\n" +
                    "5. Avoid any sensitive words or content, and use no more than three emojis in the entire email.\n" +
                    "\n" +
                    "Output Format:\n" +
                    "Return ONLY a strictly formatted JSON array where each object includes the following fields:\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"lang\": \"Primary language of the email\",\n" +
                    "    \"title\": \"Optimized email subject line\",\n" +
                    "    \"score\": 8.7\n" +
                    "  }\n" +
                    "]\n" +
                    "No explanations, comments, or additional text are allowed."
    );

    public static List<String> emailGeneratePromptListV3 = List.of(
            "你是一名专业的海外邮件文案与标题优化专家。\n" +
                    "请执行以下指令（任务编号：req-$UUID）。\n" +
                    "\n" +
                    "【风格参数设置】\n" +
                    "情感基调: $emotion\n" +
                    "功能目标: $target\n" +
                    "发件角色: $character\n" +
                    "语言风格: $style\n" +
                    "其他特征: $other\n" +
                    "其他信息录入: $message\n" +
                    "（若某项为“默认”，则忽略该维度的风格干预）\n" +
                    "\n" +
                    "以下是需要处理的邮件内容：\n" +
                    "【邮件标题】:\n" +
                    "\"\"\"\n" +
                    "$Subject\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "【标题优化规则】\n" +
                    "1. 标题应清晰、简洁、自然，不得包含营销夸张或诱导性语句。\n" +
                    "2. 禁止使用敏感词、虚假承诺、金钱诱导、强情绪或紧迫词（如“must、urgent、last chance、win、free”等）。\n" +
                    "3. 标题应保持真实语气，避免过度修饰或重复标点。\n" +
                    "4. 在确保自然与安全的前提下，增加吸引性与可读性，可通过：\n" +
                    "   - 突出关键词或职位/主题核心；\n" +
                    "   - 采用轻疑问句或引导式结构（如 “Looking for...” “Discover how...” 等）；\n" +
                    "   - 控制长度在 35–65 字符之间，提高邮件 inbox 率；\n" +
                    "   - 避免标题全部大写、重复符号或点击诱导语言。\n" +
                    "5. 优化目标为：在不触发过滤风险的前提下，提高打开率与 inbox 率，降低垃圾率。\n" +
                    "6. 若检测到原始标题存在潜在风控风险，应在保持核心信息不变的前提下，自动进行合规改写。\n" +
                    "\n" +
                    "【邮件正文】:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "【邮件正文优化规则】：\n" +
                    "1. 识别邮件标题与正文的主体语言（lang）。\n" +
                    "2. 优化后的所有版本必须保持与原文一致的语言，不得翻译或混用中英文。\n" +
                    "3. 生成 $N 个优化版本，每个版本包含标题与正文，并满足以下要求：\n" +
                    "   i. 不改变语义、语言与事实内容的前提下，对标题与正文进行措辞与结构优化，以不同表达方式呈现。\n" +
                    "   ii. 各版本之间差异度≥50%，包括语气、句式、逻辑顺序、篇幅、表达节奏等变化。\n" +
                    "   iii. 保持核心信息与品牌调性一致。\n" +
                    "   iv. 严禁更换主体语言，不得混用中英文。\n" +
                    "   v. 保留占位符（{{var}}、优惠码、链接等），emoji≤3个，若无则不添加。\n" +
                    "   vi. 对正文进行自然扩充，扩充内容须与主题相关，如：场景描写、任务细节、团队氛围、受众价值、使用体验等。\n" +
                    "   vii. 正文长度要求：\n" +
                    "       - 每个版本必须在 250~500 词范围内，但长度分布需明显不同。\n" +
                    "       - 系统需自动为每个版本分配不同篇幅等级（短 / 中 / 中长 / 长 / 超长）。\n" +
                    "       - 5 个版本的目标字数分布示例：约 260、320、380、440、500 词（可上下浮动 ±30）。\n" +
                    "       - 严禁生成长度过于接近（差距小于80词）的版本。\n" +
                    "       - 每个版本需在篇幅与节奏上体现自然差异（如细节扩充、背景补充、语气铺陈差异）。\n" +
                    "   viii. 每个版本采用不同叙述逻辑结构：\n" +
                    "        - 直接型：开头直入主题；\n" +
                    "        - 铺垫型：先背景或问题，再主旨；\n" +
                    "        - 说明型：分点展示核心要素；\n" +
                    "        - 讲述型：以自然叙述或邀请语气引导；\n" +
                    "        - 公告型：简明要点式信息；\n" +
                    "        - 总结型：先结论或机会，再补充原因与亮点。\n" +
                    "      每个版本须在逻辑结构或语气上显著不同。\n" +
                    "   ix. 为每个版本生成质量评分（1–10），表示自然度与优化质量。\n" +
                    "   x. 若与原文差异度低于50%，需重新生成。\n" +
                    "   xi. 在生成阶段，请先随机决定每个版本的语气与篇幅模式，再进行文本生成，以确保差异度来自“结构 + 节奏 + 信息量”三维度，而非仅词汇替换。\n" +
                    "\n" +
                    "风格与风控要求：\n" +
                    "1. 禁止夸张、诱导、紧迫语气。\n" +
                    "2. 不使用感叹号或强营销词。\n" +
                    "3. 保持自然真实、语气平稳。\n" +
                    "4. 若原文含链接，仅优化文字部分，不动链接结构。\n" +
                    "5. 严禁使用任何敏感、歧义、政治、宗教或成人相关内容。\n" +
                    "\n" +
                    "输出格式要求：\n" +
                    "仅返回严格格式的 JSON 数组，每个对象包含以下字段：\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"lang\": \"邮件主体语言\",\n" +
                    "    \"title\": \"优化后的邮件标题\",\n" +
                    "    \"email\": \"优化后的邮件正文\",\n" +
                    "    \"score\": 8.7\n" +
                    "  }\n" +
                    "]\n" +
                    "不得包含任何解释、注释或额外说明。",
            "You are a professional email copywriting and subject line optimization expert.\n" +
                    "Please execute the following instructions (Task ID: req-$UUID).\n" +
                    "\n" +
                    "[STYLE PARAMETERS]\n" +
                    "Emotional tone: $emotion\n" +
                    "Functional goal: $target\n" +
                    "Sender persona: $character\n" +
                    "Language style: $style\n" +
                    "Other traits: $other\n" +
                    "Additional info: $message\n" +
                    "(If any field is set to 'default', ignore that dimension in style adjustments.)\n" +
                    "\n" +
                    "Here is the email to process:\n" +
                    "[EMAIL SUBJECT]:\n" +
                    "\"\"\"\n" +
                    "$Subject\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "[SUBJECT OPTIMIZATION RULES]\n" +
                    "1. Keep the subject clear, concise, and natural; avoid exaggerated or manipulative language.\n" +
                    "2. Do not use sensitive words, false promises, monetary triggers, or urgency terms (e.g., “must,” “urgent,” “last chance,” “win,” “free”).\n" +
                    "3. Maintain a genuine tone and avoid over-decoration or excessive punctuation.\n" +
                    "4. To increase readability and open rate safely:\n" +
                    "   - Highlight key terms or topic focus.\n" +
                    "   - Use light inquiry or guiding structures (e.g., “Looking for...”, “Discover how...” ).\n" +
                    "   - Keep length between 35–65 characters to improve inbox placement.\n" +
                    "   - Avoid all-caps, repeated symbols, or clickbait expressions.\n" +
                    "5. Optimization goal: improve open rate and inbox rate while minimizing spam risk.\n" +
                    "6. If the original subject has compliance risk, rewrite naturally while preserving its meaning.\n" +
                    "\n" +
                    "[EMAIL BODY]:\n" +
                    "\"\"\"\n" +
                    "$Content\n" +
                    "\"\"\"\n" +
                    "\n" +
                    "[EMAIL BODY OPTIMIZATION RULES]\n" +
                    "1. Detect the main language (lang) of both the subject and body.\n" +
                    "2. The optimized versions must keep the same language as the original body — do not translate or mix languages.\n" +
                    "3. Generate $N optimized versions, each containing both title and body, following these rules:\n" +
                    "   i. Rephrase and restructure without altering meaning, language, or factual accuracy.\n" +
                    "   ii. Ensure ≥50% variation across versions (tone, sentence structure, order, pacing, or length).\n" +
                    "   iii. Maintain consistent brand voice and intent.\n" +
                    "   iv. Keep placeholders ({{var}}, promo codes, links) intact; limit emoji to ≤3.\n" +
                    "   v. Expand content naturally, adding context such as scenarios, details, team mood, audience value, or usage experience.\n" +
                    "   vi. Word count per version: 250–500 words, with clearly different lengths.\n" +
                    "       - Example distribution (for 5 versions): ~260, 320, 380, 440, 500 words (±30 allowed).\n" +
                    "       - Avoid versions with <80-word difference.\n" +
                    "   vii. Each version must adopt a distinct narrative logic:\n" +
                    "        - Direct: start with the main point;\n" +
                    "        - Contextual: introduce background before topic;\n" +
                    "        - Explanatory: outline key points in sections;\n" +
                    "        - Narrative: use a natural or inviting tone;\n" +
                    "        - Announce-style: concise key facts;\n" +
                    "        - Summary-first: begin with conclusion or highlight, then elaborate.\n" +
                    "   viii. Assign a quality score (1–10) for naturalness and optimization quality.\n" +
                    "   ix. If difference <50% from the original, regenerate.\n" +
                    "   x. Randomize tone and length mode before generation to ensure variation in structure + pacing + content depth, not just word swaps.\n" +
                    "\n" +
                    "[STYLE & COMPLIANCE GUIDELINES]\n" +
                    "1. Avoid exaggerated, manipulative, or urgent tone.\n" +
                    "2. No exclamation marks or aggressive marketing terms.\n" +
                    "3. Keep the tone natural and balanced.\n" +
                    "4. If links exist, optimize only the text, not the URLs.\n" +
                    "5. Never include sensitive, political, religious, or adult content.\n" +
                    "\n" +
                    "[OUTPUT FORMAT]\n" +
                    "Return a strict JSON array only, with each object containing:\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"lang\": \"Primary language of the email\",\n" +
                    "    \"title\": \"Optimized email subject\",\n" +
                    "    \"email\": \"Optimized email body\",\n" +
                    "    \"score\": 8.7\n" +
                    "  }\n" +
                    "]\n" +
                    "No explanations, comments, or extra text are allowed."
    );

    public static String emailContentOptimize(Socks5 socks5, String content, String apiKey) throws Exception {
        return emailContentOptimize(socks5, content, apiKey, null);
    }

    public static String emailContentOptimize(Socks5 socks5, String content, String apiKey, String prompt) throws Exception {
//        if (!content.contains("<body>")) {
//            content = "<body>" + content + "</body>";
//        }
//        if (!content.contains("<html")) {
//            content = "<html>" + content + "</html>";
//        }
//        if (!content.contains("<!DOCTYPE html>")) {
//            content = "<!DOCTYPE html>" + content;
//        }

        String model = "gemini-2.5-flash-lite";
        Date start = new Date();
        String proxy = socks5 == null ? "" : socks5.getIp() + ";" + socks5.getPort() + ";" + socks5.getUsername() + ";" + socks5.getPassword();
        log.info(proxy + "  " + apiKey + " 开始邮件改写," + DateUtil.formatByDate(start, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        try {
            prompt = (StringUtils.isEmpty(prompt) ? emailOptimizePromptList.get((int) Math.floor(Math.random() * emailOptimizePromptList.size())) : prompt).replace("$Content", content);
            log.info(prompt);

            List<Map<String, Object>> parts = new ArrayList();
            parts.add(Map.of("text", prompt));

            OkHttpClient googleAi = OkHttpClientFactory.getGoogleAi(socks5);
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("contents", List.of(Map.of(
                    "parts", parts,
                    "role", "user")
            )));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey);
            request = builder.post(body).build();

            log.info(proxy + "  " +apiKey + " 邮件改写 开始请求：" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            try (Response response = googleAi.newCall(request).execute()) {
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Date end = new Date();
                    log.info(proxy + "  " +apiKey + " 邮件改写,成功" + DateUtil.formatByDate(end, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                    log.info(proxy + "  " +apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
                    log.info(proxy + "  " +apiKey + " " + "result: " + respStr.replace("\n", ""));
                    if (respStr.contains("PROHIBITED_CONTENT")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "GOOGLE AI 审核提示：含有禁止内容");
                    }
                    JSONObject objRes = JSONObject.parseObject(respStr);
                    if (objRes.containsKey("error")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, objRes.getJSONObject("error").toString());
                    }
                    respStr = objRes.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                    return respStr.replace("```html", "").replace("```", "");
                }
            }
            throw new Exception("邮件改写失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(proxy + "  " +apiKey + " 结束邮件改写,失败: {}", e.getMessage());
            log.info(proxy + "  " +apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }
    public static String emailContentOptimizeV2(String content, String apiKey, int n) throws Exception {
        return emailContentOptimizeV2(content, apiKey, null, n);
    }

    public static String emailContentOptimizeV2(String content, String apiKey, String prompt, int n) throws Exception {
//        if (!content.contains("<body>")) {
//            content = "<body>" + content + "</body>";
//        }
//        if (!content.contains("<html")) {
//            content = "<html>" + content + "</html>";
//        }
//        if (!content.contains("<!DOCTYPE html>")) {
//            content = "<!DOCTYPE html>" + content;
//        }

        String model = "gemini-2.5-flash-lite";
        Date start = new Date();
        log.info(apiKey + " 开始邮件改写," + DateUtil.formatByDate(start, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        try {
            prompt = (StringUtils.isEmpty(prompt) ? emailOptimizePromptList.get((int) Math.floor(Math.random() * emailOptimizePromptList.size())) : prompt).replace("$Content", content).replace("$UUID", UUIDUtils.get32UUId()).replace("$N", String.valueOf(n));
            log.info(prompt);

            List<Map<String, Object>> parts = new ArrayList();
            parts.add(Map.of("text", prompt));

            OkHttpClient googleAi = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("contents", List.of(Map.of(
                    "parts", parts,
                    "role", "user")
            ),"generationConfig", Map.of("temperature", 0.7, "topP", 0.9, "maxOutputTokens", 8192 * 10)));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://gmail10.tnt-pub.com/api/open/v1beta/models/" + model + ":generateContent?key=" + apiKey);
            request = builder.post(body).build();

            log.info(apiKey + " 邮件改写 开始请求：" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            try (Response response = googleAi.newCall(request).execute()) {
                if (response.code() == 429) {
                    Thread.sleep(3000);
                }
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Date end = new Date();
                    log.info(apiKey + " 邮件改写,成功" + DateUtil.formatByDate(end, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                    log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
                    log.info(apiKey + " " + "result: " + respStr.replace("\n", ""));
                    if (respStr.contains("PROHIBITED_CONTENT")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "GOOGLE AI 审核提示：含有禁止内容");
                    }
                    JSONObject objRes = JSONObject.parseObject(respStr);
                    if (objRes.containsKey("error")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, objRes.getJSONObject("error").toString());
                    }
                    respStr = objRes.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                    return respStr;
                }
            }
            throw new Exception("邮件改写失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(apiKey + " 结束邮件改写,失败: {}", e.getMessage());
            log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }

    public static String emailTitleOptimizeV2(String content, String apiKey, int n) throws Exception {
        return emailTitleOptimizeV2(content, apiKey, null, n);
    }

    public static String emailTitleOptimizeV2(String title, String apiKey, String prompt, int n) throws Exception {
        String model = "gemini-2.5-flash-lite";
        Date start = new Date();
        log.info(apiKey + " 开始邮件改写," + DateUtil.formatByDate(start, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        try {
            prompt = (StringUtils.isEmpty(prompt) ? emailOptimizeTitlePromptList.get((int) Math.floor(Math.random() * emailOptimizeTitlePromptList.size())) : prompt).replace("$Content", title).replace("$UUID", UUIDUtils.get32UUId()).replace("$N", String.valueOf(n));
            log.info(prompt);

            List<Map<String, Object>> parts = new ArrayList();
            parts.add(Map.of("text", prompt));

            OkHttpClient googleAi = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("contents", List.of(Map.of(
                    "parts", parts,
                    "role", "user")
            ),"generationConfig", Map.of("temperature", 0.7, "topP", 0.9, "maxOutputTokens", 8192 * 10)));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://gmail10.tnt-pub.com/api/open/v1beta/models/" + model + ":generateContent?key=" + apiKey);
            request = builder.post(body).build();

            log.info(apiKey + " 邮件改写 开始请求：" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            try (Response response = googleAi.newCall(request).execute()) {
                if (response.code() == 429) {
                    Thread.sleep(3000);
                }
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Date end = new Date();
                    log.info(apiKey + " 邮件改写,成功" + DateUtil.formatByDate(end, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                    log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
                    log.info(apiKey + " " + "result: " + respStr.replace("\n", ""));
                    if (respStr.contains("PROHIBITED_CONTENT")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "GOOGLE AI 审核提示：含有禁止内容");
                    }
                    JSONObject objRes = JSONObject.parseObject(respStr);
                    if (objRes.containsKey("error")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, objRes.getJSONObject("error").toString());
                    }
                    respStr = objRes.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                    return respStr;
                }
            }
            throw new Exception("邮件改写失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(apiKey + " 结束邮件改写,失败: {}", e.getMessage());
            log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }

    public static String emailContentGenerate(String content, String apiKey, String prompt, int n) throws Exception {
        String model = "gemini-2.5-flash-lite";
        Date start = new Date();
        log.info(apiKey + " 开始邮件改写," + DateUtil.formatByDate(start, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        try {
            prompt = (StringUtils.isEmpty(prompt) ? emailGeneratePromptList.get((int) Math.floor(Math.random() * emailGeneratePromptList.size())) : prompt).replace("$Content", content).replace("$UUID", UUIDUtils.get32UUId()).replace("$N", String.valueOf(n));
            log.info(prompt);

            List<Map<String, Object>> parts = new ArrayList();
            parts.add(Map.of("text", prompt));

            OkHttpClient googleAi = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("contents", List.of(Map.of(
                    "parts", parts,
                    "role", "user")
            ),"generationConfig", Map.of("temperature", 0.7, "topP", 0.9, "maxOutputTokens", 8192 * 10)));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://gmail10.tnt-pub.com/api/open/v1beta/models/" + model + ":generateContent?key=" + apiKey);
            request = builder.post(body).build();

            log.info(apiKey + " 邮件改写 开始请求：" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            try (Response response = googleAi.newCall(request).execute()) {
                if (response.code() == 429) {
                    Thread.sleep(3000);
                }
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Date end = new Date();
                    log.info(apiKey + " 邮件改写,成功" + DateUtil.formatByDate(end, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                    log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
                    log.info(apiKey + " " + "result: " + respStr.replace("\n", ""));
                    if (respStr.contains("PROHIBITED_CONTENT")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "GOOGLE AI 审核提示：含有禁止内容");
                    }
                    JSONObject objRes = JSONObject.parseObject(respStr);
                    if (objRes.containsKey("error")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, objRes.getJSONObject("error").toString());
                    }
                    respStr = objRes.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                    return respStr;
                }
            }
            throw new Exception("邮件改写失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(apiKey + " 结束邮件改写,失败: {}", e.getMessage());
            log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }

    public static String emailContentGenerateV2(String content, String subject, Map<String, String> param, String apiKey, String prompt, int n) throws Exception {
        String model = "gemini-2.5-flash-lite";
        Date start = new Date();
        log.info(apiKey + " 开始邮件改写," + DateUtil.formatByDate(start, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        try {
            prompt = (StringUtils.isEmpty(prompt) ? emailGeneratePromptListV3.get((int) Math.floor(Math.random() * emailGeneratePromptListV3.size())) : prompt)
                    .replace("$Content", content).replace("$Subject", subject).replace("$UUID", UUIDUtils.get32UUId()).replace("$N", String.valueOf(n));
            for (String key: param.keySet()) {
                prompt = prompt.replace("$" + key, param.get(key));
            }
            log.info(prompt);

            List<Map<String, Object>> parts = new ArrayList();
            parts.add(Map.of("text", prompt));

            OkHttpClient googleAi = OkHttpClientFactory.getDefaultClient();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("contents", List.of(Map.of(
                    "parts", parts,
                    "role", "user")
            ),"generationConfig", Map.of("temperature", 0.7, "topP", 0.9, "maxOutputTokens", 8192 * 10)));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://gmail10.tnt-pub.com/api/open/v1beta/models/" + model + ":generateContent?key=" + apiKey);
            request = builder.post(body).build();

            log.info(apiKey + " 邮件改写 开始请求：" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            try (Response response = googleAi.newCall(request).execute()) {
                if (response.code() == 429) {
                    Thread.sleep(3000);
                }
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Date end = new Date();
                    log.info(apiKey + " 邮件改写,成功" + DateUtil.formatByDate(end, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
                    log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
                    log.info(apiKey + " " + "result: " + respStr.replace("\n", ""));
                    if (respStr.contains("PROHIBITED_CONTENT")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "GOOGLE AI 审核提示：含有禁止内容");
                    }
                    JSONObject objRes = JSONObject.parseObject(respStr);
                    if (objRes.containsKey("error")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, objRes.getJSONObject("error").toString());
                    }
                    respStr = objRes.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                    return respStr;
                }
            }
            throw new Exception("邮件改写失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(apiKey + " 结束邮件改写,失败: {}", e.getMessage());
            log.info(apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }

    @Data
    public static class Result {
        private Integer ageLow;
        private Integer ageHigh;
        private String gender;
        private String ethnicity;
        private String hairColor;
        private String skinColor;
        private String md5;
        private String error;
        private String url;
    }

    private static final Map<String, String> imageCache = new ConcurrentHashMap<>();

    public static byte[] readBytes(String path) throws IOException, URISyntaxException {
        if (path.startsWith("http")) {
            return ImageValidator.downloadAndValidateImage(path);
        } else {
            // 本地文件读取
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                throw new FileNotFoundException("File not found: " + path);
            }

            try (InputStream fis = new FileInputStream(file);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                return out.toByteArray();
            }
        }
    }

    public static Map<String, String> promptMap = Map.of("v1", "You are given $N input images. For each image, detect all human faces and estimate age, gender, and ethnicity for each face. Return the results in a strict JSON 2D array of length $N, where each sub-array contains the detection results for one image.\n" +
            "\n" +
            "Constraints:\n" +
            "- The outer array must have exactly $N elements — one for each input image, in order.\n" +
            "- If an image has no detectable faces, return an empty array at that index.\n" +
            "- Each face result must be a JSON object with \"age\" (integer), \"gender\" (\"male\" or \"female\"), and \"ethnicity\" (\"Asian\", \"White\", \"Black\", \"Hispanic\", etc).\n" +
            "- Do not merge or skip any images. Do not return fewer or more items than the number of input images.\n" +
            "\n" +
            "Strictly return only the 2D JSON array, with no additional text or explanation.\n");

    public static List<String> promptList = List.of(
            "任务（ID：req-$UUID）\n" +
                    "你将获得 $N 张输入图像。对于每张图像，检测最多 3 张最显著的人脸，并估算每张人脸的以下属性：\n" +
                    "\n" +
                    "年龄（age）：整数（使用真实、变化的数值）\n" +
                    "\n" +
                    "性别（gender）：\"male\" 或 \"female\"\n" +
                    "\n" +
                    "种族（ethnicity）：例如 \"Asian\"、\"White\"、\"Black\"、\"Hispanic\" 等\n" +
                    "\n" +
                    "发色（hairColor）：例如 \"black\"、\"brown\"、\"blonde\"、\"white\"、\"gray\" 等\n" +
                    "\n" +
                    "肤色（skinColor）：例如 \"fair\"、\"light\"、\"medium\"、\"tan\"、\"brown\"、\"dark\" 等\n" +
                    "\n" +
                    "输出格式\n" +
                    "返回一个长度为 $N 的二维 JSON 数组。每个元素对应一张图像，顺序与输入一致。\n" +
                    "\n" +
                    "如果未检测到人脸，则该图像对应一个空数组 []。\n" +
                    "\n" +
                    "每个内层数组最多包含 3 个人脸对象。\n" +
                    "\n" +
                    "每个人脸对象必须只包含以下键：\"age\"、\"gender\"、\"hairColor\"、\"skinColor\" 和 \"ethnicity\"。\n" +
                    "\n" +
                    "约束条件\n" +
                    "\n" +
                    "输出必须是严格有效的 JSON，不允许有额外注释。\n" +
                    "\n" +
                    "外层数组必须恰好包含 $N 个元素。\n" +
                    "\n" +
                    "人脸属性必须真实可信，避免使用占位符或默认值，如年龄为 0 或 5。",

                "タスク（ID：req-$UUID）  \n" +
                        "あなたは $N 枚の入力画像を受け取ります。各画像について、最大 3 つの最も目立つ顔を検出し、各顔の以下の属性を推定してください。\n" +
                        "\n" +
                        "年齢（age）：整数（現実的でバリエーションのある値を使用）  \n" +
                        "\n" +
                        "性別（gender）：\"male\" または \"female\"  \n" +
                        "\n" +
                        "人種（ethnicity）：例：\"Asian\"、\"White\"、\"Black\"、\"Hispanic\" など  \n" +
                        "\n" +
                        "髪の色（hairColor）：例：\"black\"、\"brown\"、\"blonde\"、\"white\"、\"gray\" など  \n" +
                        "\n" +
                        "肌の色（skinColor）：例：\"fair\"、\"light\"、\"medium\"、\"tan\"、\"brown\"、\"dark\" など  \n" +
                        "\n" +
                        "出力形式  \n" +
                        "長さ $N の二次元 JSON 配列を返してください。各要素は画像 1 枚に対応し、入力の順序と一致させます。\n" +
                        "\n" +
                        "顔が検出されなかった場合、その画像に対応する要素は空配列 [] としてください。\n" +
                        "\n" +
                        "各内側の配列には最大 3 つの顔オブジェクトを含めることができます。\n" +
                        "\n" +
                        "各顔オブジェクトには、以下のキーのみを含めてください：\"age\"、\"gender\"、\"hairColor\"、\"skinColor\"、\"ethnicity\"。\n" +
                        "\n" +
                        "制約条件\n" +
                        "\n" +
                        "出力は厳密に有効な JSON でなければならず、追加のコメントは許可されません。\n" +
                        "\n" +
                        "外側の配列には正確に $N 個の要素を含める必要があります。\n" +
                        "\n" +
                        "顔の属性は現実的で信頼できるものでなければならず、年齢が 0 や 5 のようなプレースホルダーやデフォルト値を使用しないでください。",
//            "Task (ID: req-$UUID)\n" +
//                    "You are provided with $N input images. For each image, detect up to 3 most prominent human faces, and estimate the following attributes for each face:\n" +
//                    "\n" +
//                    "\"age\": an integer (use realistic, varied values)\n" +
//                    "\n" +
//                    "\"gender\": \"male\" or \"female\"\n" +
//                    "\n" +
//                    "\"ethnicity\": one of \"Asian\", \"White\", \"Black\", \"Hispanic\", etc.\n" +
//                    "\n" +
//                    "\"hairColor\": one of \"black\", \"brown\", \"blonde\", \"white\",\"gray\", etc.\n" +
//                    "\n" +
//                    "\"skinColor\": one of \"fair\", \"light\", \"medium\", \"tan\", \"brown\",\"dark\", etc.\n" +
//                    "\n" +
//                    "Output Format\n" +
//                    "Return a 2D JSON array of length $N. Each element corresponds to one image, in the same order.\n" +
//                    "\n" +
//                    "If no face is detected, insert an empty array [] for that image.\n" +
//                    "\n" +
//                    "Each inner array may contain up to 3 face objects.\n" +
//                    "\n" +
//                    "Each face object must include only the keys: \"age\", \"gender\", \"hairColor\", \"skinColor\", and \"ethnicity\".\n" +
//                    "\n" +
//                    "Constraints\n" +
//                    "\n" +
//                    "Output must be strictly valid JSON, with no extra commentary.\n" +
//                    "\n" +
//                    "The outer array must contain exactly $N elements.\n" +
//                    "\n" +
//                    "Face attributes must appear realistic. Avoid placeholder or default values like 0 or 5 for age.\n",
                    // "\n" +
                    // "Example Output\n" +
                    // "\n" +
                    // "[\n" +
                    // "  [],\n" +
                    // "  [\n" +
                    // "    { \"age\": 27, \"gender\": \"female\", \"ethnicity\": \"White\" }\n" +
                    // "  ],\n" +
                    // "  [\n" +
                    // "    { \"age\": 33, \"gender\": \"male\", \"ethnicity\": \"Black\" },\n" +
                    // "    { \"age\": 24, \"gender\": \"female\", \"ethnicity\": \"Asian\" }\n" +
                    // "  ]\n" +
                    // "]",
            "任务（req-$UUID）：请执行以下指令。\n" +
                    "\n" +
                    "你将获得 $N 张图像。对于每张图像，检测 最多 3 张显著的人脸。对于每张检测到的人脸，估算以下属性：\n" +
                    "\n" +
                    "年龄（age）：整数，真实且自然变化\n" +
                    "\n" +
                    "性别（gender）：\"male\" 或 \"female\"\n" +
                    "\n" +
                    "种族（ethnicity）：例如 \"Asian\"、\"White\"、\"Black\"、\"Hispanic\" 等\n" +
                    "\n" +
                    "发色（hairColor）：例如 \"black\"、\"brown\"、\"blonde\"、\"white\"、\"gray\" 等\n" +
                    "\n" +
                    "肤色（skinColor）：例如 \"fair\"、\"light\"、\"medium\"、\"tan\"、\"brown\" 等\n" +
                    "\n" +
                    "输出为 严格的二维 JSON 数组，长度为 $N，要求：\n" +
                    "\n" +
                    "每个元素对应一张输入图像，保持原始顺序。\n" +
                    "\n" +
                    "每个元素为最多包含 3 个面部对象的数组；如果未检测到人脸，则为空数组。\n" +
                    "\n" +
                    "额外指南：\n" +
                    "\n" +
                    "每张图像最多估算 3 张人脸。\n" +
                    "\n" +
                    "年龄估算应尽可能准确、自然，避免集中在典型的圆整数字上。\n" +
                    "\n" +
                    "不要包含任何文本、元数据或注释。仅输出 原始 JSON 数组。",

                "작업 (req-$UUID): 다음 지침을 수행해 주세요.\n" +
                        "\n" +
                        "당신은 $N 장의 이미지를 받게 됩니다. 각 이미지에 대해 최대 3개의 눈에 띄는 얼굴을 감지하고, 감지된 각 얼굴에 대해 다음 속성을 추정합니다:\n" +
                        "\n" +
                        "나이(age): 정수, 현실적이고 자연스러운 변화 값  \n" +
                        "\n" +
                        "성별(gender): \"male\" 또는 \"female\"  \n" +
                        "\n" +
                        "인종(ethnicity): 예: \"Asian\", \"White\", \"Black\", \"Hispanic\" 등  \n" +
                        "\n" +
                        "머리 색(hairColor): 예: \"black\", \"brown\", \"blonde\", \"white\", \"gray\" 등  \n" +
                        "\n" +
                        "피부 색(skinColor): 예: \"fair\", \"light\", \"medium\", \"tan\", \"brown\" 등  \n" +
                        "\n" +
                        "출력은 엄격한 2차원 JSON 배열이어야 하며, 길이는 $N 입니다. 요구 사항:\n" +
                        "\n" +
                        "각 요소는 입력 이미지 1장에 대응하며, 원래 순서를 유지합니다.  \n" +
                        "\n" +
                        "각 요소는 최대 3개의 얼굴 객체를 포함하는 배열이며, 얼굴이 감지되지 않은 경우 빈 배열([])이어야 합니다.  \n" +
                        "\n" +
                        "추가 안내:\n" +
                        "\n" +
                        "각 이미지에서 최대 3개의 얼굴만 추정합니다.  \n" +
                        "\n" +
                        "나이 추정은 가능한 한 정확하고 자연스러워야 하며, 일반적인 둥근 숫자(예: 0, 5)에 집중하지 마세요.  \n" +
                        "\n" +
                        "텍스트, 메타데이터 또는 주석을 포함하지 마세요. 오직 원본 JSON 배열만 출력합니다.",

//            "Task (req-$UUID): Please process the following instruction.\n" +
//                    "\n" +
//                    "You are given $N images. For each image, detect **up to 3 prominent human faces**. For each detected face, estimate the following attributes: \n" +
//                    "- \"age\" (integer, realistic, and naturally varied)\n" +
//                    "- \"gender\" (\"male\" or \"female\")\n" +
//                    "- \"ethnicity\" (e.g., \"Asian\", \"White\", \"Black\", \"Hispanic\", etc.)\n" +
//                    "- \"hairColor\" (e.g., \"black\", \"brown\", \"blonde\", \"white\",\"gray\", etc.)\n" +
//                    "- \"skinColor\" (e.g., \"fair\", \"light\", \"medium\", \"tan\", \"brown\", etc.)\n" +
//                    "\n" +
//                    "Output a **strict 2D JSON array of length $N**, where:\n" +
//                    "- Each element corresponds to the respective input image, preserving original order.\n" +
//                    "- Each element is an array of up to 3 face objects, or an empty array if no face is detected.\n" +
//                    "\n" +
//                    "Additional guidelines:\n" +
//                    "- Do not estimate more than 3 faces per image.\n" +
//                    "- Age estimates should be as accurate and human-like as possible. Avoid artificial clustering around typical round numbers.\n" +
//                    "- Do not include any text, metadata, or commentary. Output **only** the raw JSON array.\n",
                    // "\n" +
                    // "Example output format:\n" +
                    // "[\n" +
                    // "  [],\n" +
                    // "  [\n" +
                    // "    { \"age\": 27, \"gender\": \"female\", \"ethnicity\": \"White\" }\n" +
                    // "  ],\n" +
                    // "  [\n" +
                    // "    { \"age\": 33, \"gender\": \"male\", \"ethnicity\": \"Black\" },\n" +
                    // "    { \"age\": 24, \"gender\": \"female\", \"ethnicity\": \"Asian\" }\n" +
                    // "  ]\n" +
                    // "]",

            "执行以下任务（任务编号：req-$UUID）。\n" +
                    "\n" +
                    "你将获得 $N 张输入图像。对于每张图像，检测所有可见的人脸，并估算 最多 3 张最显著人脸 的以下属性：\n" +
                    "\n" +
                    "年龄（age）：以整数形式返回，避免圆整值。年龄估算应自然、真实，反映典型的年龄变化。\n" +
                    "\n" +
                    "性别（gender）：必须为 \"male\" 或 \"female\"。\n" +
                    "\n" +
                    "种族（ethnicity）：使用广泛类别，如 \"Asian\"、\"White\"、\"Black\"、\"Latino\"、\"Indian\" 等。\n" +
                    "\n" +
                    "发色（hairColor）：使用广泛类别，如 \"black\"、\"brown\"、\"blonde\"、\"white\"、\"gray\" 等。\n" +
                    "\n" +
                    "肤色（skinColor）：使用广泛类别，如 \"fair\"、\"light\"、\"medium\"、\"tan\"、\"brown\" 等。\n" +
                    "\n" +
                    "返回结果为 二维 JSON 数组，长度为 $N，每个元素对应一张图像，包含检测到的人脸数组（最多 3 个对象）。如果图像中未检测到人脸，则该位置返回空数组 []。\n" +
                    "\n" +
                    "约束条件：\n" +
                    "\n" +
                    "输出顺序必须严格与输入图像顺序一致。\n" +
                    "\n" +
                    "不得遗漏或重新排序结果。\n" +
                    "\n" +
                    "仅返回最终 JSON 数组，不添加任何文本或解释。",

                "Exécutez la tâche suivante (ID de tâche : req-$UUID).\n" +
                        "\n" +
                        "Vous recevrez $N images d'entrée. Pour chaque image, détectez tous les visages visibles et estimez les attributs suivants pour **au maximum 3 des visages les plus saillants** :\n" +
                        "\n" +
                        "Âge (age) : renvoyé sous forme d'entier, en évitant les valeurs arrondies. L'estimation de l'âge doit être naturelle et réaliste, reflétant des variations typiques d'âge.\n" +
                        "\n" +
                        "Genre (gender) : doit être \"male\" ou \"female\".\n" +
                        "\n" +
                        "Ethnicité (ethnicity) : utilisez des catégories larges, telles que \"Asian\", \"White\", \"Black\", \"Latino\", \"Indian\", etc.\n" +
                        "\n" +
                        "Couleur des cheveux (hairColor) : utilisez des catégories larges, telles que \"black\", \"brown\", \"blonde\", \"white\", \"gray\", etc.\n" +
                        "\n" +
                        "Couleur de peau (skinColor) : utilisez des catégories larges, telles que \"fair\", \"light\", \"medium\", \"tan\", \"brown\", etc.\n" +
                        "\n" +
                        "Le résultat doit être un tableau JSON à deux dimensions, de longueur $N. Chaque élément correspond à une image et contient un tableau des visages détectés (jusqu'à 3 objets). Si aucun visage n'est détecté dans une image, cet élément doit être un tableau vide [].\n" +
                        "\n" +
                        "Contraintes :\n" +
                        "\n" +
                        "L'ordre de sortie doit correspondre strictement à l'ordre des images d'entrée.\n" +
                        "\n" +
                        "Aucun résultat ne doit être omis ou réordonné.\n" +
                        "\n" +
                        "Ne renvoyez que le tableau JSON final, sans ajouter de texte ou d'explications.",

//            "Execute the following task (Task No.: req-$UUID).\n" +
//                    "\n" +
//                    "You are provided with $N input images. For each image, detect all visible human faces and estimate the following attributes for **up to 3 most prominent faces**:\n" +
//                    "- **age**: Return as an integer, but avoid rounded values. Age estimates must appear natural and realistic, reflecting typical age variations.\n" +
//                    "- **gender**: Must be either \"male\" or \"female\".\n" +
//                    "- **ethnicity**: Use broad categories such as \"Asian\", \"White\", \"Black\", \"Latino\", \"Indian\", etc.\n" +
//                    "- **hairColor**: Use broad categories such as \"black\", \"brown\", \"blonde\", \"white\",\"gray\", etc.\n" +
//                    "- **skinColor**: Use broad categories such as \"fair\", \"light\", \"medium\", \"tan\", \"brown\", etc.\n" +
//                    "\n" +
//                    "Return the result as a **2D JSON array** of length $N, where each element corresponds to one image and contains an array of detected face results (up to 3 items). If no faces are detected in an image, return an empty array `[]` at that position.\n" +
//                    "\n" +
//                    "**Constraints:**\n" +
//                    "- Output order must strictly match the input image order.\n" +
//                    "- Do not omit or reorder results.\n" +
//                    "- Only return the final JSON array. No additional text or explanation.\n",
                    // "\n" +
                    // "Example output format:\n" +
                    // "[\n" +
                    // "  [],\n" +
                    // "  [\n" +
                    // "    { \"age\": 27, \"gender\": \"female\", \"ethnicity\": \"White\" }\n" +
                    // "  ],\n" +
                    // "  [\n" +
                    // "    { \"age\": 33, \"gender\": \"male\", \"ethnicity\": \"Black\" },\n" +
                    // "    { \"age\": 24, \"gender\": \"female\", \"ethnicity\": \"Asian\" }\n" +
                    // "  ]\n" +
                    // "]",

            "指令（任务编号：req-$UUID）：执行以下图像处理任务。\n" +
                    "\n" +
                    "你将获得 $N 张图像。对于每张图像：\n" +
                    "\n" +
                    "检测所有可见的人脸（每张图像最多 3 张，以最显著的人脸为优先）。\n" +
                    "\n" +
                    "对每张检测到的人脸，估算以下属性：\n" +
                    "\n" +
                    "年龄（age）：整数\n" +
                    "\n" +
                    "性别（gender）： \"male\" 或 \"female\"\n" +
                    "\n" +
                    "种族（ethnicity）：例如 \"white\"、\"black\"、\"asian\"、\"indian\"、\"latino\"、\"middle eastern\"、\"other\"\n" +
                    "\n" +
                    "发色（hairColor）：例如 \"black\"、\"brown\"、\"blonde\"、\"white\"、\"gray\" 等\n" +
                    "\n" +
                    "肤色（skinColor）：例如 \"fair\"、\"light\"、\"medium\"、\"tan\"、\"brown\" 等\n" +
                    "\n" +
                    "格式要求：\n" +
                    "\n" +
                    "输出必须为严格的二维 JSON 数组，长度为 $N（每张图像对应一个数组）。\n" +
                    "\n" +
                    "如果图像中未检测到人脸，则返回空数组。\n" +
                    "\n" +
                    "每张人脸结果必须为 JSON 对象，包含键：\"age\"、\"gender\"、\"hairColor\"、\"skinColor\" 和 \"ethnicity\"。\n" +
                    "\n" +
                    "严格保持输入图像顺序。\n" +
                    "\n" +
                    "⚠\uFE0F 仅输出 JSON 数组，不要包含任何解释或额外文本。",
                "Инструкция (ID задачи: req-$UUID): выполните следующую задачу обработки изображений.\n" +
                        "\n" +
                        "Вы получите $N изображений. Для каждого изображения:\n" +
                        "\n" +
                        "- Обнаружьте все видимые лица (не более 3 на изображение, приоритет — наиболее заметные лица).  \n" +
                        "\n" +
                        "- Для каждого обнаруженного лица оцените следующие атрибуты:\n" +
                        "\n" +
                        "Возраст (age): целое число  \n" +
                        "\n" +
                        "Пол (gender): \"male\" или \"female\"  \n" +
                        "\n" +
                        "Этническая принадлежность (ethnicity): например \"white\", \"black\", \"asian\", \"indian\", \"latino\", \"middle eastern\", \"other\"  \n" +
                        "\n" +
                        "Цвет волос (hairColor): например \"black\", \"brown\", \"blonde\", \"white\", \"gray\"  \n" +
                        "\n" +
                        "Цвет кожи (skinColor): например \"fair\", \"light\", \"medium\", \"tan\", \"brown\"  \n" +
                        "\n" +
                        "Требования к формату:\n" +
                        "\n" +
                        "Вывод должен быть строго двухмерным JSON массивом длиной $N (каждое изображение соответствует одному массиву).  \n" +
                        "\n" +
                        "Если на изображении лица не обнаружены, возвращайте пустой массив [].  \n" +
                        "\n" +
                        "Каждый результат для лица должен быть JSON объектом с ключами: \"age\", \"gender\", \"hairColor\", \"skinColor\" и \"ethnicity\".  \n" +
                        "\n" +
                        "Строго соблюдайте порядок входных изображений.  \n" +
                        "\n" +
                        "⚠\uFE0F Выводите только JSON массив, без каких-либо объяснений или дополнительного текста.",
//            "Instruction (Task ID: req-$UUID): Perform the following image processing task.\n" +
//                    "\n" +
//                    "You are given $N images. For each image:\n" +
//                    "- Detect all visible human faces (up to 3 per image, prioritizing the most prominent).\n" +
//                    "- For each detected face, estimate:\n" +
//                    "  - \"age\" as an integer.\n" +
//                    "  - \"gender\" as either \"male\" or \"female\".\n" +
//                    "  - \"ethnicity\" (e.g., \"white\", \"black\", \"asian\", \"indian\", \"latino\", \"middle eastern\", \"other\").\n" +
//                    "  - \"hairColor\" (e.g., \"black\", \"brown\", \"blonde\", \"white\",\"gray\", etc.)\n" +
//                    "  - \"skinColor\" (e.g., \"fair\", \"light\", \"medium\", \"tan\", \"brown\", etc.)\n" +
//                    "\n" +
//                    "**Format Requirements:**\n" +
//                    "- Output must be a strict 2D JSON array of length $N (one array per image).\n" +
//                    "- If no face is detected in an image, return an empty array for that image.\n" +
//                    "- Each face result must be a JSON object with keys: \"age\", \"gender\", \"hairColor\", \"skinColor\", and \"ethnicity\".\n" +
//                    "- Preserve input image order exactly.\n" +
//                    "\n" +
//                    "⚠\uFE0F Output only the JSON array. Do not include any explanation or additional text.\n",
                    // "Example output format:\n" +
                    // "[\n" +
                    // "  [],\n" +
                    // "  [\n" +
                    // "    { \"age\": 27, \"gender\": \"female\", \"ethnicity\": \"White\" }\n" +
                    // "  ],\n" +
                    // "  [\n" +
                    // "    { \"age\": 33, \"gender\": \"male\", \"ethnicity\": \"Black\" },\n" +
                    // "    { \"age\": 24, \"gender\": \"female\", \"ethnicity\": \"Asian\" }\n" +
                    // "  ]\n" +
                    // "]",


            "完成以下任务（参考编号：req-$UUID）。\n" +
                    "\n" +
                    "你将获得 $N 张图像。对于每张图像：\n" +
                    "\n" +
                    "检测所有可见的人脸（每张图像最多 3 张，以最显著的人脸为优先）。\n" +
                    "\n" +
                    "对每张检测到的人脸，估算以下属性：\n" +
                    "\n" +
                    "年龄（age）：整数，反映真实的人类年龄（使用自然、多样的数值）。\n" +
                    "\n" +
                    "性别（gender）： \"male\" 或 \"female\"。\n" +
                    "\n" +
                    "种族（ethnicity）：提供最可能的种族标签。\n" +
                    "\n" +
                    "发色（hairColor）：提供发色。\n" +
                    "\n" +
                    "肤色（skinColor）：提供肤色。\n" +
                    "\n" +
                    "返回 二维 JSON 数组，长度为 $N。每个元素为：\n" +
                    "\n" +
                    "包含最多 3 个面部对象的数组。\n" +
                    "\n" +
                    "如果未检测到人脸，则为一个空数组。\n" +
                    "\n" +
                    "约束条件：\n" +
                    "\n" +
                    "输出数组长度必须严格等于 $N。\n" +
                    "\n" +
                    "每个面部对象必须包含所有字段：\"age\"、\"gender\"、\"hairColor\"、\"skinColor\" 和 \"ethnicity\"。\n" +
                    "\n" +
                    "输出顺序必须严格与图像顺序一致，不得打乱、跳过或合并结果。\n" +
                    "\n" +
                    "严格仅输出 JSON 数组 — 不添加任何额外文本、说明或格式。",

                "Führen Sie die folgende Aufgabe aus (Referenz-ID: req-$UUID).\n" +
                        "\n" +
                        "Sie erhalten $N Bilder. Für jedes Bild:\n" +
                        "\n" +
                        "- Erkennen Sie alle sichtbaren Gesichter (maximal 3 pro Bild, Priorität auf die auffälligsten Gesichter).  \n" +
                        "\n" +
                        "- Für jedes erkannte Gesicht schätzen Sie die folgenden Attribute:\n" +
                        "\n" +
                        "Alter (age): Ganzzahl, die das tatsächliche menschliche Alter widerspiegelt (verwenden Sie natürliche, vielfältige Werte).  \n" +
                        "\n" +
                        "Geschlecht (gender): \"male\" oder \"female\".  \n" +
                        "\n" +
                        "Ethnie (ethnicity): Geben Sie die wahrscheinlichste ethnische Zugehörigkeit an.  \n" +
                        "\n" +
                        "Haarfarbe (hairColor): Geben Sie die Haarfarbe an.  \n" +
                        "\n" +
                        "Hautfarbe (skinColor): Geben Sie die Hautfarbe an.  \n" +
                        "\n" +
                        "Geben Sie ein zweidimensionales JSON-Array zurück, Länge $N. Jedes Element ist:\n" +
                        "\n" +
                        "- Ein Array mit maximal 3 Gesichtsobjekten.  \n" +
                        "- Wenn keine Gesichter erkannt werden, geben Sie ein leeres Array [] zurück.  \n" +
                        "\n" +
                        "Einschränkungen:\n" +
                        "\n" +
                        "Die Länge des Ausgabearrays muss genau $N betragen.  \n" +
                        "\n" +
                        "Jedes Gesichtsobjekt muss alle Felder enthalten: \"age\", \"gender\", \"hairColor\", \"skinColor\" und \"ethnicity\".  \n" +
                        "\n" +
                        "Die Ausgabereihenfolge muss genau der Reihenfolge der Eingabebilder entsprechen. Ergebnisse dürfen nicht vertauscht, übersprungen oder zusammengeführt werden.  \n" +
                        "\n" +
                        "Streng nur das JSON-Array ausgeben — keine zusätzlichen Texte, Erklärungen oder Formatierungen.",
//            "Complete the following task (ref: req-$UUID).\n" +
//                    "\n" +
//                    "You are given $N images. For each image:\n" +
//                    "- Detect all visible human faces (up to 3 per image, prioritizing the most prominent).\n" +
//                    "- For each detected face, estimate the following attributes:\n" +
//                    "  - \"age\": an integer with realistic human age (use varied, natural values).\n" +
//                    "  - \"gender\": either \"male\" or \"female\".\n" +
//                    "  - \"ethnicity\": provide the most probable ethnic group label.\n" +
//                    "  - \"hairColor\": provide hair color.\n" +
//                    "  - \"skinColor\": provide skin color.\n" +
//                    "\n" +
//                    "Return a **2D JSON array** of length $N. Each element is:\n" +
//                    "- An array of up to 3 face objects.\n" +
//                    "- An empty array if no face is detected.\n" +
//                    "\n" +
//                    "**Constraints:**\n" +
//                    "- Output array length must exactly equal $N.\n" +
//                    "- Each face object must include all 3 fields: \"age\", \"gender\", \"hairColor\", \"skinColor\", and \"ethnicity\".\n" +
//                    "- Keep the output strictly aligned with the image order — do not shuffle, skip, or merge results.\n" +
//                    "\n" +
//                    "**Output strictly the JSON array only — no extra text, explanations, or formatting.**\n",
                    // "Example output format:\n" +
                    // "[\n" +
                    // "  [],\n" +
                    // "  [\n" +
                    // "    { \"age\": 27, \"gender\": \"female\", \"ethnicity\": \"White\" }\n" +
                    // "  ],\n" +
                    // "  [\n" +
                    // "    { \"age\": 33, \"gender\": \"male\", \"ethnicity\": \"Black\" },\n" +
                    // "    { \"age\": 24, \"gender\": \"female\", \"ethnicity\": \"Asian\" }\n" +
                    // "  ]\n" +
                    // "]",

            "任务编号：req-$UUID\n" +
                    "\n" +
                    "你将获得 $N 张输入图像。对于每张图像，检测人脸，并返回每张检测到的人脸的以下估算属性，结果以结构化二维 JSON 数组形式返回（长度为 $N）：\n" +
                    "\n" +
                    "年龄（age）：整数，范围 1–100，尽可能准确预测\n" +
                    "\n" +
                    "性别（gender）：\"male\" 或 \"female\"\n" +
                    "\n" +
                    "种族（ethnicity）：表示估计种族的字符串标签\n" +
                    "\n" +
                    "发色（hairColor）：表示估计发色的字符串标签\n" +
                    "\n" +
                    "肤色（skinColor）：表示估计肤色的字符串标签\n" +
                    "\n" +
                    "规则：\n" +
                    "\n" +
                    "输出必须是二维 JSON 数组，严格包含 $N 个元素，每个元素对应一张图像。\n" +
                    "\n" +
                    "每张图像最多检测 3 张人脸，优先考虑最显著的人脸。\n" +
                    "\n" +
                    "对于未检测到人脸的图像，返回空数组 []。\n" +
                    "\n" +
                    "每张检测到的人脸必须以 JSON 对象形式返回上述所有字段。\n" +
                    "\n" +
                    "输出中不得包含任何额外文本或说明，仅返回有效的二维 JSON 数组。\n" +
                    "\n" +
                    "输出顺序必须严格与输入图像顺序一致。",

                "رقم المهمة: req-$UUID\n" +
                        "\n" +
                        "ستتلقى $N صورة إدخال. لكل صورة، اكتشف الوجوه وأرجع لكل وجه تم اكتشافه الخصائص المقدرة التالية في شكل مصفوفة JSON ثنائية الأبعاد منظمة (الطول = $N):\n" +
                        "\n" +
                        "العمر (age): عدد صحيح، نطاق 1–100، تقدير دقيق قدر الإمكان  \n" +
                        "\n" +
                        "الجنس (gender): \"male\" أو \"female\"  \n" +
                        "\n" +
                        "العرق (ethnicity): وسم نصي يمثل العرق المقدر  \n" +
                        "\n" +
                        "لون الشعر (hairColor): وسم نصي يمثل لون الشعر المقدر  \n" +
                        "\n" +
                        "لون البشرة (skinColor): وسم نصي يمثل لون البشرة المقدر  \n" +
                        "\n" +
                        "القواعد:\n" +
                        "\n" +
                        "- يجب أن يكون الإخراج مصفوفة JSON ثنائية الأبعاد، تحتوي بالضبط على $N عنصرًا، كل عنصر يمثل صورة واحدة.  \n" +
                        "- يتم اكتشاف ما يصل إلى 3 وجوه لكل صورة، مع إعطاء الأولوية للوجوه الأكثر بروزًا.  \n" +
                        "- إذا لم يتم اكتشاف أي وجوه في الصورة، يتم إرجاع مصفوفة فارغة [].  \n" +
                        "- يجب أن يتم إرجاع كل وجه مكتشف ككائن JSON يحتوي على جميع الحقول المذكورة أعلاه.  \n" +
                        "- لا يُسمح بأي نص إضافي أو شروح في الإخراج، ويجب إرجاع مصفوفة JSON ثنائية الأبعاد صالحة فقط.  \n" +
                        "- يجب أن يتطابق ترتيب الإخراج بدقة مع ترتيب صور الإدخال.",
//            "Task ID: req-$UUID\n" +
//                    "You are given $N input images. For each image, detect human faces and return the estimated age (as an integer), gender, hair color, skin color, and ethnicity for each detected face in a structured 2D JSON array (length $N).\n" +
//                    "\n" +
//                    "Rules:\n" +
//                    "\n" +
//                    "The output must be a 2D JSON array with exactly $N elements, each corresponding to one image.\n" +
//                    "\n" +
//                    "Detect no more than 3 faces per image, prioritizing the most prominent ones.\n" +
//                    "\n" +
//                    "For any image without faces, return an empty array [].\n" +
//                    "\n" +
//                    "For each detected face, return a JSON object with the following fields:\n" +
//                    "\n" +
//                    "\"age\": an integer between 1–100, predicted as accurately as possible.\n" +
//                    "\n" +
//                    "\"gender\": \"male\" or \"female\"\n" +
//                    "\n" +
//                    "\"ethnicity\": a string label indicating estimated ethnicity\n" +
//                    "\n" +
//                    "\"hairColor\": a string label indicating estimated hair color.\n" +
//                    "\n" +
//                    "\"skinColor\": a string label indicating estimated skin color.\n" +
//                    "\n" +
//                    "Output must not contain any extra text or explanation—only the valid 2D JSON array.\n" +
//                    "\n" +
//                    "Output order must strictly match the input image order.\n",
                    // "Example output format:\n" +
                    // "[\n" +
                    // "  [],\n" +
                    // "  [\n" +
                    // "    { \"age\": 27, \"gender\": \"female\", \"ethnicity\": \"White\" }\n" +
                    // "  ],\n" +
                    // "  [\n" +
                    // "    { \"age\": 33, \"gender\": \"male\", \"ethnicity\": \"Black\" },\n" +
                    // "    { \"age\": 24, \"gender\": \"female\", \"ethnicity\": \"Asian\" }\n" +
                    // "  ]\n" +
                    // "]",

            "执行以下操作（任务编号：req-$UUID）。  \n" +
                    "你将获得 $N 张图像进行分析。请检测人脸，并为每张人脸返回年龄、性别、发色、肤色和种族的估算值，结果以长度为 $N 的二维 JSON 数组返回。  \n" +
                    "\n" +
                    "规格要求：\n" +
                    "\n" +
                    "- 每个子数组严格对应一张输入图像。  \n" +
                    "- 每张图像最多检测 3 张人脸，如果超过三张，则优先返回最显著的面部。  \n" +
                    "- 如果未检测到人脸，则返回空子数组 []。  \n" +
                    "- 每个面部对象必须为：{\"age\": 整数, \"gender\": \"male\" 或 \"female\", \"ethnicity\": 字符串, \"hairColor\": 字符串, \"skinColor\": 字符串}。  \n" +
                    "- \"age\" 应为尽可能精确的整数估算值。  \n" +
                    "- 不允许遗漏或多余项，输出顺序必须严格与输入图像顺序一致。  \n" +
                    "- 除了结构化 JSON 数组外，不得返回任何其他内容。",

            "Realice la siguiente operación (Número de tarea: req-$UUID).  \n" +
                    "Recibirá $N imágenes para analizar. Detecte los rostros humanos y devuelva las estimaciones de edad, género, color de cabello, color de piel y etnia para cada rostro en un arreglo JSON 2D de longitud $N.  \n" +
                    "\n" +
                    "Especificaciones:\n" +
                    "\n" +
                    "- Cada subarreglo corresponde estrictamente a una imagen de entrada.  \n" +
                    "- Detecte hasta 3 rostros por imagen; si hay más de tres, priorice los rostros más prominentes.  \n" +
                    "- Si no se detecta ningún rostro, devuelva un subarreglo vacío [].  \n" +
                    "- Cada objeto de rostro debe ser: {\"age\": entero, \"gender\": \"male\" o \"female\", \"ethnicity\": cadena, \"hairColor\": cadena, \"skinColor\": cadena}.  \n" +
                    "- \"age\" debe ser un entero estimado con la mayor precisión posible.  \n" +
                    "- No se permiten elementos faltantes ni adicionales; el orden de salida debe coincidir estrictamente con el orden de las imágenes de entrada.  \n" +
                    "- No devuelva nada más que el arreglo JSON estructurado.",

            "Perform the following operation (Task No.: req-$UUID).\n" +
                    "You have $N images to analyze. Detect human faces and return age, gender, hair color, skin color, and ethnicity estimates for each face in a 2D JSON array of length $N.\n" +
                    "\n" +
                    "Specifications:\n" +
                    "\n" +
                    "Each sub-array corresponds exactly to one input image.\n" +
                    "\n" +
                    "Detect up to 3 faces per image, prioritizing the most prominent faces if more than three are present.\n" +
                    "\n" +
                    "Return an empty sub-array if no faces are detected.\n" +
                    "\n" +
                    "Each face object must be: {\"age\": int, \"gender\": \"male\" or \"female\", \"ethnicity\": string, \"hairColor\": string, \"skinColor\": string}.\n" +
                    "\n" +
                    "The \"age\" should be an integer estimated as precisely as possible.\n" +
                    "\n" +
                    "No missing or extra items allowed; output order must strictly match the input image order.\n" +
                    "\n" +
                    "Do not return anything other than the structured JSON array.\n",
                    // "Example output format:\n" +
                    // "[\n" +
                    // "  [],\n" +
                    // "  [\n" +
                    // "    { \"age\": 27, \"gender\": \"female\", \"ethnicity\": \"White\" }\n" +
                    // "  ],\n" +
                    // "  [\n" +
                    // "    { \"age\": 33, \"gender\": \"male\", \"ethnicity\": \"Black\" },\n" +
                    // "    { \"age\": 24, \"gender\": \"female\", \"ethnicity\": \"Asian\" }\n" +
                    // "  ]\n" +
                    // "]",

            "请执行以下指令（任务编号：req-$UUID）。\n" +
                    "你将获得 $N 张输入图像。请检测每张图像中的所有人脸，并估算其年龄、性别、发色、肤色和种族。输出一个严格的二维 JSON 数组，长度为 $N，每个元素对应一张图像。\n" +
                    "\n" +
                    "约束条件：\n" +
                    "\n" +
                    "外层数组必须严格包含 $N 个元素，顺序与图像一致。\n" +
                    "\n" +
                    "每张图像最多检测 3 张人脸，若多于 3 张，仅返回最显著的 3 张。\n" +
                    "\n" +
                    "若某张图像未检测到人脸，则对应位置为一个空数组 []。\n" +
                    "\n" +
                    "每个人脸的 JSON 对象必须包含字段：\n" +
                    "\n" +
                    "\"age\"：整数，年龄预测须尽量准确。\n" +
                    "\n" +
                    "\"gender\"：\"male\" 或 \"female\"\n" +
                    "\n" +
                    "\"ethnicity\"：字符串，如 \"Asian\"、\"White\"、\"Black\"、\"Hispanic\" 等。\n" +
                    "\n" +
                    "\"hairColor\"：字符串，如 \"black\"、\"brown\"、 \"blonde\"、\"white\"、\"gray\" 等。\n" +
                    "\n" +
                    "\"skinColor\"：字符串，如 \"fair\"、\"light\"、\"medium\"、\"tan\"、\"brown\" 等。\n" +
                    "\n" +
                    "输出结果数量必须与输入图像数量完全一致，顺序一一对应。\n" +
                    "\n" +
                    "只返回严格格式的 JSON 数组，不得包含任何额外文字或解释。\n",
                    // "\n" +
                    // "示例输出格式：\n" + "[\n" +
                    // "  [],\n" +
                    // "  [\n" +
                    // "    { \"age\": 27, \"gender\": \"female\", \"ethnicity\": \"White\" }\n" +
                    // "  ],\n" +
                    // "  [\n" +
                    // "    { \"age\": 33, \"gender\": \"male\", \"ethnicity\": \"Black\" },\n" +
                    // "    { \"age\": 24, \"gender\": \"female\", \"ethnicity\": \"Asian\" }\n" +
                    // "  ]\n" +
                    // "]",

            "以下の指示（タスクID：req-$UUID）を実行してください。  \n" +
                    "$N 枚の入力画像が与えられます。各画像内のすべての顔を検出し、それぞれの年齢、性別、髪色、肌色、民族を推定してください。出力は厳密な二次元 JSON 配列とし、長さは $N、各要素は1枚の画像に対応します。  \n" +
                    "\n" +
                    "制約条件：  \n" +
                    "\n" +
                    "- 外側の配列は厳密に $N 個の要素を含み、画像の順序と一致させること。  \n" +
                    "- 各画像で検出できる顔は最大3つまで。3つ以上ある場合は、最も顕著な3つのみを返すこと。  \n" +
                    "- 画像に顔が検出されなかった場合、対応する要素は空配列 `[]` とすること。  \n" +
                    "- 各顔の JSON オブジェクトには以下のフィールドを含めること：  \n" +
                    "\n" +
                    "  - `\"age\"`：整数。年齢予測はできるだけ正確に行い、0、5、10、15、20 のような端数を避けること。ただし、実際に該当する場合は例外。  \n" +
                    "  - `\"gender\"`：`\"male\"` または `\"female\"`  \n" +
                    "  - `\"ethnicity\"`：文字列、例：`\"Asian\"`、`\"White\"`、`\"Black\"`、`\"Hispanic\"` など  \n" +
                    "  - `\"hairColor\"`：文字列、例：`\"black\"`、`\"brown\"`、`\"blonde\"`、`\"white\"`、`\"gray\"` など  \n" +
                    "  - `\"skinColor\"`：文字列、例：`\"fair\"`、`\"light\"`、`\"medium\"`、`\"tan\"`、`\"brown\"` など  \n" +
                    "\n" +
                    "- 出力結果の数は入力画像の数と完全に一致し、順序も対応させること。  \n" +
                    "- 厳密な JSON 配列のみを返し、追加の文章や説明は含めないこと。",


            "다음 지시사항(작업 ID: req-$UUID)을 실행하십시오.  \n" +
                    "$N장의 입력 이미지가 제공됩니다. 각 이미지에서 모든 얼굴을 감지하고, 나이, 성별, 머리 색, 피부 색, 인종을 추정하십시오. 출력은 엄격한 2차원 JSON 배열이어야 하며, 길이는 $N이고 각 요소는 하나의 이미지에 대응합니다.  \n" +
                    "\n" +
                    "제약 조건:  \n" +
                    "\n" +
                    "- 외부 배열은 반드시 $N개의 요소를 포함해야 하며, 이미지 순서와 일치해야 합니다.  \n" +
                    "- 각 이미지에서 감지할 수 있는 얼굴은 최대 3개까지입니다. 3개 이상인 경우 가장 두드러진 3개만 반환하십시오.  \n" +
                    "- 이미지에서 얼굴이 감지되지 않은 경우, 해당 위치는 빈 배열 `[]`로 표시합니다.  \n" +
                    "- 각 얼굴의 JSON 객체는 다음 필드를 포함해야 합니다:  \n" +
                    "\n" +
                    "  - `\"age\"`: 정수. 나이 추정은 가능한 한 정확하게 수행하며, 0, 5, 10, 15, 20과 같은 반올림 숫자는 실제에 해당하지 않는 한 피하십시오.  \n" +
                    "  - `\"gender\"`: `\"male\"` 또는 `\"female\"`  \n" +
                    "  - `\"ethnicity\"`: 문자열, 예: `\"Asian\"`, `\"White\"`, `\"Black\"`, `\"Hispanic\"` 등  \n" +
                    "  - `\"hairColor\"`: 문자열, 예: `\"black\"`, `\"brown\"`, `\"blonde\"`, `\"white\"`, `\"gray\"` 등  \n" +
                    "  - `\"skinColor\"`: 문자열, 예: `\"fair\"`, `\"light\"`, `\"medium\"`, `\"tan\"`, `\"brown\"` 등  \n" +
                    "\n" +
                    "- 출력 결과의 수는 입력 이미지 수와 정확히 일치해야 하며, 순서도 대응해야 합니다.  \n" +
                    "- 엄격한 JSON 배열만 반환하며, 추가 텍스트나 설명은 포함하지 마십시오.",


            "Veuillez exécuter les instructions suivantes (ID de tâche : req-$UUID).  \n" +
                    "Vous recevrez $N images d’entrée. Détectez tous les visages humains dans chaque image et estimez leur âge, sexe, couleur de cheveux, couleur de peau et origine ethnique. La sortie doit être un tableau JSON 2D strict de longueur $N, chaque élément correspondant à une image.  \n" +
                    "\n" +
                    "Contraintes :  \n" +
                    "\n" +
                    "- Le tableau extérieur doit contenir strictement $N éléments, dans le même ordre que les images.  \n" +
                    "- Détectez au maximum 3 visages par image. Si plus de 3 visages sont présents, ne retournez que les 3 visages les plus saillants.  \n" +
                    "- Si aucun visage n’est détecté dans une image, l’élément correspondant doit être un tableau vide `[]`.  \n" +
                    "- Chaque objet JSON représentant un visage doit contenir les champs suivants :  \n" +
                    "\n" +
                    "  - `\"age\"` : entier. La prédiction de l’âge doit être aussi précise que possible, évitez les nombres ronds comme 0, 5, 10, 15, 20, sauf si cela correspond réellement.  \n" +
                    "  - `\"gender\"` : `\"male\"` ou `\"female\"`  \n" +
                    "  - `\"ethnicity\"` : chaîne de caractères, par exemple `\"Asian\"`, `\"White\"`, `\"Black\"`, `\"Hispanic\"`, etc.  \n" +
                    "  - `\"hairColor\"` : chaîne de caractères, par exemple `\"black\"`, `\"brown\"`, `\"blonde\"`, `\"white\"`, `\"gray\"`, etc.  \n" +
                    "  - `\"skinColor\"` : chaîne de caractères, par exemple `\"fair\"`, `\"light\"`, `\"medium\"`, `\"tan\"`, `\"brown\"`, etc.  \n" +
                    "\n" +
                    "- Le nombre d’éléments de sortie doit correspondre exactement au nombre d’images d’entrée, dans le même ordre.  \n" +
                    "- Retournez uniquement un tableau JSON strict, sans texte ou explications supplémentaires.",

            "Bitte führen Sie die folgenden Anweisungen aus (Aufgaben-ID: req-$UUID).  \n" +
                    "Sie erhalten $N Eingabebilder. Erkennen Sie alle menschlichen Gesichter in jedem Bild und schätzen Sie deren Alter, Geschlecht, Haarfarbe, Hautfarbe und Ethnie. Die Ausgabe muss ein strikt 2D JSON-Array der Länge $N sein, wobei jedes Element einem Bild entspricht.  \n" +
                    "\n" +
                    "Einschränkungen:  \n" +
                    "\n" +
                    "- Das äußere Array muss strikt $N Elemente enthalten, in der gleichen Reihenfolge wie die Bilder.  \n" +
                    "- Erkennen Sie maximal 3 Gesichter pro Bild. Wenn mehr als 3 Gesichter vorhanden sind, geben Sie nur die 3 auffälligsten Gesichter zurück.  \n" +
                    "- Wenn in einem Bild kein Gesicht erkannt wird, sollte das entsprechende Element ein leeres Array `[]` sein.  \n" +
                    "- Jedes JSON-Objekt eines Gesichts muss die folgenden Felder enthalten:  \n" +
                    "\n" +
                    "  - `\"age\"`: Ganzzahl. Die Altersschätzung sollte so genau wie möglich sein. Vermeiden Sie runde Zahlen wie 0, 5, 10, 15, 20, es sei denn, sie entsprechen tatsächlich der Realität.  \n" +
                    "  - `\"gender\"`: `\"male\"` oder `\"female\"`  \n" +
                    "  - `\"ethnicity\"`: Zeichenkette, z. B. `\"Asian\"`, `\"White\"`, `\"Black\"`, `\"Hispanic\"` usw.  \n" +
                    "  - `\"hairColor\"`: Zeichenkette, z. B. `\"black\"`, `\"brown\"`, `\"blonde\"`, `\"white\"`, `\"gray\"` usw.  \n" +
                    "  - `\"skinColor\"`: Zeichenkette, z. B. `\"fair\"`, `\"light\"`, `\"medium\"`, `\"tan\"`, `\"brown\"` usw.  \n" +
                    "\n" +
                    "- Die Anzahl der Ausgabeelemente muss genau mit der Anzahl der Eingabebilder übereinstimmen und die Reihenfolge muss beibehalten werden.  \n" +
                    "- Geben Sie nur ein strikt formatiertes JSON-Array zurück, ohne zusätzlichen Text oder Erklärungen.",

            "Пожалуйста, выполните следующие инструкции (ID задачи: req-$UUID).  \n" +
                    "Вам предоставляется $N входных изображений. Обнаружьте все человеческие лица на каждом изображении и оцените их возраст, пол, цвет волос, цвет кожи и этническую принадлежность. Выходные данные должны быть строго двумерным JSON-массивом длиной $N, каждый элемент которого соответствует одному изображению.  \n" +
                    "\n" +
                    "Ограничения:  \n" +
                    "\n" +
                    "- Внешний массив должен строго содержать $N элементов, в том же порядке, что и изображения.  \n" +
                    "- Обнаруживайте не более 3 лиц на изображение. Если лиц больше 3, возвращайте только 3 наиболее заметных.  \n" +
                    "- Если на изображении не обнаружено лиц, соответствующий элемент должен быть пустым массивом `[]`.  \n" +
                    "- Каждый JSON-объект лица должен содержать следующие поля:  \n" +
                    "\n" +
                    "  - `\"age\"`: целое число. Прогноз возраста должен быть как можно более точным, избегайте круглых чисел, таких как 0, 5, 10, 15, 20, если это не соответствует реальности.  \n" +
                    "  - `\"gender\"`: `\"male\"` или `\"female\"`  \n" +
                    "  - `\"ethnicity\"`: строка, например `\"Asian\"`, `\"White\"`, `\"Black\"`, `\"Hispanic\"` и т.д.  \n" +
                    "  - `\"hairColor\"`: строка, например `\"black\"`, `\"brown\"`, `\"blonde\"`, `\"white\"`, `\"gray\"` и т.д.  \n" +
                    "  - `\"skinColor\"`: строка, например `\"fair\"`, `\"light\"`, `\"medium\"`, `\"tan\"`, `\"brown\"` и т.д.  \n" +
                    "\n" +
                    "- Количество элементов в выводе должно точно соответствовать количеству входных изображений, и порядок должен совпадать.  \n" +
                    "- Возвращайте только строго отформатированный JSON-массив, без какого-либо дополнительного текста или объяснений.",


            "Por favor, ejecute las siguientes instrucciones (ID de tarea: req-$UUID).  \n" +
                    "Recibirá $N imágenes de entrada. Detecte todos los rostros humanos en cada imagen y estime su edad, género, color de cabello, color de piel y etnia. La salida debe ser un arreglo JSON 2D estricto de longitud $N, donde cada elemento corresponde a una imagen.  \n" +
                    "\n" +
                    "Restricciones:  \n" +
                    "\n" +
                    "- El arreglo externo debe contener estrictamente $N elementos, en el mismo orden que las imágenes.  \n" +
                    "- Detecte un máximo de 3 rostros por imagen. Si hay más de 3, devuelva únicamente los 3 rostros más prominentes.  \n" +
                    "- Si no se detecta ningún rostro en una imagen, el elemento correspondiente debe ser un arreglo vacío `[]`.  \n" +
                    "- Cada objeto JSON de un rostro debe contener los siguientes campos:  \n" +
                    "\n" +
                    "  - `\"age\"`: entero. La predicción de la edad debe ser lo más precisa posible; evite números redondos como 0, 5, 10, 15, 20, a menos que realmente correspondan.  \n" +
                    "  - `\"gender\"`: `\"male\"` o `\"female\"`  \n" +
                    "  - `\"ethnicity\"`: cadena de texto, por ejemplo `\"Asian\"`, `\"White\"`, `\"Black\"`, `\"Hispanic\"`, etc.  \n" +
                    "  - `\"hairColor\"`: cadena de texto, por ejemplo `\"black\"`, `\"brown\"`, `\"blonde\"`, `\"white\"`, `\"gray\"`, etc.  \n" +
                    "  - `\"skinColor\"`: cadena de texto, por ejemplo `\"fair\"`, `\"light\"`, `\"medium\"`, `\"tan\"`, `\"brown\"`, etc.  \n" +
                    "\n" +
                    "- La cantidad de elementos de salida debe coincidir exactamente con la cantidad de imágenes de entrada, manteniendo el mismo orden.  \n" +
                    "- Devuelva únicamente un arreglo JSON estrictamente formateado, sin texto adicional ni explicaciones.",

            "Si prega di eseguire le seguenti istruzioni (ID attività: req-$UUID).  \n" +
                    "Riceverai $N immagini di input. Rileva tutti i volti umani in ciascuna immagine e stima la loro età, genere, colore dei capelli, colore della pelle ed etnia. L’output deve essere un array JSON 2D rigoroso di lunghezza $N, in cui ogni elemento corrisponde a un’immagine.  \n" +
                    "\n" +
                    "Vincoli:  \n" +
                    "\n" +
                    "- L’array esterno deve contenere rigorosamente $N elementi, nello stesso ordine delle immagini.  \n" +
                    "- Rileva un massimo di 3 volti per immagine. Se ci sono più di 3 volti, restituisci solo i 3 volti più evidenti.  \n" +
                    "- Se in un’immagine non viene rilevato alcun volto, l’elemento corrispondente deve essere un array vuoto `[]`.  \n" +
                    "- Ogni oggetto JSON di un volto deve contenere i seguenti campi:  \n" +
                    "\n" +
                    "  - `\"age\"`: numero intero. La stima dell’età deve essere il più accurata possibile; evita numeri tondi come 0, 5, 10, 15, 20, a meno che non corrispondano realmente alla realtà.  \n" +
                    "  - `\"gender\"`: `\"male\"` o `\"female\"`  \n" +
                    "  - `\"ethnicity\"`: stringa, ad esempio `\"Asian\"`, `\"White\"`, `\"Black\"`, `\"Hispanic\"`, ecc.  \n" +
                    "  - `\"hairColor\"`: stringa, ad esempio `\"black\"`, `\"brown\"`, `\"blonde\"`, `\"white\"`, `\"gray\"`, ecc.  \n" +
                    "  - `\"skinColor\"`: stringa, ad esempio `\"fair\"`, `\"light\"`, `\"medium\"`, `\"tan\"`, `\"brown\"`, ecc.  \n" +
                    "\n" +
                    "- Il numero di elementi di output deve corrispondere esattamente al numero di immagini di input, mantenendo lo stesso ordine.  \n" +
                    "- Restituisci solo un array JSON rigorosamente formattato, senza testo aggiuntivo o spiegazioni.",

            "يرجى تنفيذ التعليمات التالية (معرّف المهمة: req-$UUID).  \n" +
                    "ستتلقى $N صورة إدخال. قم باكتشاف جميع الوجوه البشرية في كل صورة، وقم بتقدير العمر والجنس ولون الشعر ولون البشرة والانتماء العرقي لكل وجه. يجب أن تكون النتيجة مصفوفة JSON ثنائية الأبعاد صارمة بطول $N، حيث يمثل كل عنصر صورة واحدة.  \n" +
                    "\n" +
                    "القيود:  \n" +
                    "\n" +
                    "- يجب أن تحتوي المصفوفة الخارجية بدقة على $N عنصرًا، بنفس ترتيب الصور.  \n" +
                    "- اكتشف ما يصل إلى 3 وجوه لكل صورة. إذا كان هناك أكثر من 3 وجوه، أعد فقط الثلاثة الأبرز.  \n" +
                    "- إذا لم يتم اكتشاف أي وجه في صورة ما، يجب أن يكون العنصر المقابل مصفوفة فارغة `[]`.  \n" +
                    "- يجب أن يحتوي كل كائن JSON للوجه على الحقول التالية:  \n" +
                    "\n" +
                    "  - `\"age\"`: عدد صحيح. يجب أن يكون تقدير العمر دقيقًا قدر الإمكان، وتجنب الأرقام المستديرة مثل 0، 5، 10، 15، 20، إلا إذا كانت مطابقة فعليًا للواقع.  \n" +
                    "  - `\"gender\"`: `\"male\"` أو `\"female\"`  \n" +
                    "  - `\"ethnicity\"`: سلسلة نصية، مثل `\"Asian\"`، `\"White\"`، `\"Black\"`، `\"Hispanic\"`، إلخ.  \n" +
                    "  - `\"hairColor\"`: سلسلة نصية، مثل `\"black\"`، `\"brown\"`، `\"blonde\"`، `\"white\"`، `\"gray\"`، إلخ.  \n" +
                    "  - `\"skinColor\"`: سلسلة نصية، مثل `\"very fair\"`، `\"fair\"`، `\"light\"`، `\"medium\"`، `\"tan\"`، `\"brown\"`، إلخ.  \n" +
                    "\n" +
                    "- يجب أن يتطابق عدد عناصر الإخراج تمامًا مع عدد صور الإدخال، مع الحفاظ على نفس الترتيب.  \n" +
                    "- أعد فقط مصفوفة JSON منسقة بدقة، دون أي نصوص أو شروحات إضافية.",

            "任务（req-$UUID）：请执行以下指令。\n" +
                    "分析 $N 张图像，识别每张图像中最多 3 张可见人脸。对每张人脸，估算其年龄、性别、肤色、发色与种族，并返回一个二维 JSON 数组（长度 = $N），每个子数组对应一张图像的识别结果。\n" +
                    "\n" +
                    "要求：\n" +
                    "\n" +
                    "最外层数组长度必须为 $N，与图像数量严格一致，顺序一致。\n" +
                    "\n" +
                    "图像中无人脸时，返回空数组 []。\n" +
                    "\n" +
                    "每张人脸以 JSON 对象表示，字段包括：\n" +
                    "\n" +
                    "\"age\"：整数，需真实可信，避免使用 0、5、10 等刻度化数字\n" +
                    "\n" +
                    "\"gender\"：字符串，取值 \"male\" 或 \"female\"\n" +
                    "\n" +
                    "\"ethnicity\"：如 \"Asian\"、\"White\"、\"Black\"、\"Hispanic\" 等\n" +
                    "\n" +
                    "\"skinColor\"：如 \"fair\"、\"light\"、\"medium\"、\"tan\"、\"brown\" 等\n" +
                    "\n" +
                    "\"hairColor\"：如 \"black\"、\"brown\"、 \"blonde\"、\"white\"、\"gray\" 等\n" +
                    "\n" +
                    "每张图像最多返回 3 张人脸的信息。\n" +
                    "\n" +
                    "只输出原始 JSON 数组，不得包含任何额外信息或说明。\n",
                    // "示例输出格式：\n" +
                    // "[\n" +
                    // "  [],\n" +
                    // "  [\n" +
                    // "    { \"age\": 27, \"gender\": \"female\", \"ethnicity\": \"White\" }\n" +
                    // "  ],\n" +
                    // "  [\n" +
                    // "    { \"age\": 33, \"gender\": \"male\", \"ethnicity\": \"Black\" },\n" +
                    // "    { \"age\": 24, \"gender\": \"female\", \"ethnicity\": \"Asian\" }\n" +
                    // "  ]\n" +
                    // "]",

                "タスク（req-$UUID）：以下の指示を実行してください。\n" +
                        "$N 枚の画像を分析し、各画像から最大 3 つの可視顔を検出します。各顔について、年齢、性別、肌の色、髪の色、民族を推定し、2 次元 JSON 配列（長さ = $N）で返してください。各サブ配列は対応する画像の認識結果を表します。\n" +
                        "\n" +
                        "要件：\n" +
                        "\n" +
                        "最外層の配列の長さは必ず $N と一致し、画像の順序と厳密に対応してください。\n" +
                        "\n" +
                        "画像に顔が存在しない場合は、空配列 [] を返してください。\n" +
                        "\n" +
                        "各顔は JSON オブジェクトで表し、以下のフィールドを含めてください：\n" +
                        "\n" +
                        "\"age\"：整数で、現実的かつ信頼性のある値。0、5、10 などの刻み値は避けてください。\n" +
                        "\n" +
                        "\"gender\"：文字列で、\"male\" または \"female\" のいずれか。\n" +
                        "\n" +
                        "\"ethnicity\"：\"Asian\"、\"White\"、\"Black\"、\"Hispanic\" など。\n" +
                        "\n" +
                        "\"skinColor\"：\"fair\"、\"light\"、\"medium\"、\"tan\"、\"brown\" など。\n" +
                        "\n" +
                        "\"hairColor\"：\"black\"、\"brown\"、\"blonde\"、\"white\"、\"gray\" など。\n" +
                        "\n" +
                        "各画像から返す顔情報は最大 3 件まで。\n" +
                        "\n" +
                        "出力は元の JSON 配列のみとし、追加情報や説明は含めないでください。",

                "작업(req-$UUID): 다음 지침을 수행하세요.\n" +
                        "$N장의 이미지를 분석하고, 각 이미지에서 최대 3개의 보이는 얼굴을 감지합니다. 각 얼굴에 대해 나이, 성별, 피부색, 머리색, 인종을 추정하고, 2차원 JSON 배열(길이 = $N)로 반환하세요. 각 하위 배열은 해당 이미지의 인식 결과를 나타냅니다.\n" +
                        "\n" +
                        "요구 사항:\n" +
                        "\n" +
                        "외부 배열의 길이는 반드시 $N과 일치해야 하며, 이미지 순서와 정확히 대응해야 합니다.\n" +
                        "\n" +
                        "이미지에 얼굴이 없는 경우, 빈 배열 []을 반환하세요.\n" +
                        "\n" +
                        "각 얼굴은 JSON 객체로 표시하며, 다음 필드를 포함해야 합니다:\n" +
                        "\n" +
                        "\"age\": 정수로, 현실적이고 신뢰할 수 있는 값이어야 합니다. 0, 5, 10 등의 계단식 숫자는 피하세요.\n" +
                        "\n" +
                        "\"gender\": 문자열로 \"male\" 또는 \"female\" 중 하나.\n" +
                        "\n" +
                        "\"ethnicity\": \"Asian\", \"White\", \"Black\", \"Hispanic\" 등.\n" +
                        "\n" +
                        "\"skinColor\": \"fair\", \"light\", \"medium\", \"tan\", \"brown\" 등.\n" +
                        "\n" +
                        "\"hairColor\": \"black\", \"brown\", \"blonde\", \"white\", \"gray\" 등.\n" +
                        "\n" +
                        "각 이미지에서 반환할 얼굴 정보는 최대 3개까지입니다.\n" +
                        "\n" +
                        "출력은 원본 JSON 배열만 포함해야 하며, 추가 정보나 설명은 포함하지 마세요.",

                "Tâche (req-$UUID) : Veuillez exécuter les instructions suivantes.\n" +
                        "Analysez $N images et détectez jusqu'à 3 visages visibles par image. Pour chaque visage, estimez l'âge, le sexe, la couleur de peau, la couleur des cheveux et l'ethnicité, puis renvoyez un tableau JSON à deux dimensions (longueur = $N). Chaque sous-tableau correspond aux résultats de reconnaissance pour une image.\n" +
                        "\n" +
                        "Exigences :\n" +
                        "\n" +
                        "La longueur du tableau externe doit être exactement $N, correspondant strictement au nombre et à l'ordre des images.\n" +
                        "\n" +
                        "Si aucune face n'est présente dans l'image, renvoyez un tableau vide [].\n" +
                        "\n" +
                        "Chaque visage doit être représenté par un objet JSON avec les champs suivants :\n" +
                        "\n" +
                        "\"age\" : entier, valeur réaliste et crédible. Évitez les chiffres ronds comme 0, 5, 10.\n" +
                        "\n" +
                        "\"gender\" : chaîne de caractères, \"male\" ou \"female\".\n" +
                        "\n" +
                        "\"ethnicity\" : par exemple \"Asian\", \"White\", \"Black\", \"Hispanic\", etc.\n" +
                        "\n" +
                        "\"skinColor\" : par exemple \"fair\", \"light\", \"medium\", \"tan\", \"brown\", etc.\n" +
                        "\n" +
                        "\"hairColor\" : par exemple \"black\", \"brown\", \"blonde\", \"white\", \"gray\", etc.\n" +
                        "\n" +
                        "Chaque image peut renvoyer au maximum 3 visages.\n" +
                        "\n" +
                        "Ne retournez que le tableau JSON original, sans aucune information ou explication supplémentaire.",

                "Задание (req-$UUID): Пожалуйста, выполните следующие инструкции.\n" +
                        "Проанализируйте $N изображений и обнаружьте до 3 видимых лиц на каждом изображении. Для каждого лица оцените возраст, пол, цвет кожи, цвет волос и этническую принадлежность, и верните двумерный JSON-массив (длина = $N). Каждый подмассив соответствует результатам распознавания для одного изображения.\n" +
                        "\n" +
                        "Требования:\n" +
                        "\n" +
                        "Длина внешнего массива должна точно соответствовать $N и строго совпадать с количеством и порядком изображений.\n" +
                        "\n" +
                        "Если на изображении нет лиц, верните пустой массив [].\n" +
                        "\n" +
                        "Каждое лицо должно быть представлено объектом JSON с следующими полями:\n" +
                        "\n" +
                        "\"age\": целое число, реалистичное и достоверное. Избегайте округлых чисел, таких как 0, 5, 10.\n" +
                        "\n" +
                        "\"gender\": строка, \"male\" или \"female\".\n" +
                        "\n" +
                        "\"ethnicity\": например \"Asian\", \"White\", \"Black\", \"Hispanic\" и т.д.\n" +
                        "\n" +
                        "\"skinColor\": например \"fair\", \"light\", \"medium\", \"tan\", \"brown\" и т.д.\n" +
                        "\n" +
                        "\"hairColor\": например \"black\", \"brown\", \"blonde\", \"white\", \"gray\", и т.д.\n" +
                        "\n" +
                        "Максимальное количество лиц, которое можно вернуть для одного изображения — 3.\n" +
                        "\n" +
                        "Выводите только исходный JSON-массив, без какой-либо дополнительной информации или пояснений.",

                "Aufgabe (req-$UUID): Bitte führen Sie die folgenden Anweisungen aus.\n" +
                        "Analysieren Sie $N Bilder und erkennen Sie bis zu 3 sichtbare Gesichter pro Bild. Für jedes Gesicht schätzen Sie Alter, Geschlecht, Hautfarbe, Haarfarbe und Ethnie und geben ein zweidimensionales JSON-Array (Länge = $N) zurück. Jedes Unterarray entspricht den Erkennungsergebnissen für ein Bild.\n" +
                        "\n" +
                        "Anforderungen:\n" +
                        "\n" +
                        "Die Länge des äußeren Arrays muss genau $N betragen und strikt der Anzahl und Reihenfolge der Bilder entsprechen.\n" +
                        "\n" +
                        "Wenn kein Gesicht auf dem Bild vorhanden ist, geben Sie ein leeres Array [] zurück.\n" +
                        "\n" +
                        "Jedes Gesicht wird als JSON-Objekt dargestellt und enthält die folgenden Felder:\n" +
                        "\n" +
                        "\"age\": ganze Zahl, realistisch und glaubwürdig. Vermeiden Sie runde Zahlen wie 0, 5, 10.\n" +
                        "\n" +
                        "\"gender\": Zeichenkette, \"male\" oder \"female\".\n" +
                        "\n" +
                        "\"ethnicity\": z. B. \"Asian\", \"White\", \"Black\", \"Hispanic\" usw.\n" +
                        "\n" +
                        "\"skinColor\": z. B. \"fair\", \"light\", \"medium\", \"tan\", \"brown\" usw.\n" +
                        "\n" +
                        "\"hairColor\": z. B. \"black\", \"brown\", \"blonde\", \"white\", \"gray\" usw.\n" +
                        "\n" +
                        "Pro Bild dürfen maximal 3 Gesichter zurückgegeben werden.\n" +
                        "\n" +
                        "Geben Sie nur das ursprüngliche JSON-Array aus, ohne zusätzliche Informationen oder Erklärungen."
            );


    public static String prompt = "You are given $N input images. For each image, detect all human faces and estimate age, gender, and ethnicity for each face. Return the results in a strict JSON 2D array of length $N, where each sub-array contains the detection results for one image.\n" +
            "\n" +
            "Constraints:\n" +
            "- The outer array must have exactly $N elements — one for each input image, in order.\n" +
            "- If an image has no detectable faces, return an empty array at that index.\n" +
            "- Each face result must be a JSON object with \"age\" (integer), \"gender\" (\"male\" or \"female\"), and \"ethnicity\" (\"Asian\", \"White\", \"Black\", \"Hispanic\", etc).\n" +
            "- Do not merge or skip any images. Do not return fewer or more items than the number of input images.\n" +
            "\n" +
            "Strictly return only the 2D JSON array, with no additional text or explanation.\n";
            /**
            "You are given multiple images. For each image, detect all human faces and analyze each face to estimate age, gender, and ethnicity. Return the results as a JSON 2D array, where each sub-array corresponds exactly to one input image and contains the face analysis results for that image.\n" +
            "\n" +
            "Important:\n" +
            "- The outer array length must exactly match the number of input images.\n" +
            "- If no faces are detected in an image, return an empty array [] at that position.\n" +
            "- Each detected face must be returned as a JSON object with the following keys:\n" +
            "  - \"age\": estimated age in years (integer),\n" +
            "  - \"gender\": either \"male\" or \"female\",\n" +
            "  - \"ethnicity\": one of the common categories (e.g., \"Asian\", \"White\", \"Black\", \"Hispanic\", etc).\n" +
            "\n" +
            "Strictly return only the JSON 2D array. Do not add any explanation, description, or extra output.\n"; */
            /*
            "For each input image, detect all human faces and analyze each face to estimate age, gender, and ethnicity. Return the result as a JSON 2D array, where each sub-array corresponds to one input image and contains the detected face data for that image. If an image has no detectable faces, return an empty array for that image. The length of the outer array must exactly match the number of input images. Each face detection result should be a JSON object with the following fields:\n" +
            "- \"age\": estimated age in years (integer),\n" +
            "- \"gender\": \"male\" or \"female\",\n" +
            "- \"ethnicity\": such as \"Asian\", \"White\", \"Black\", \"Hispanic\", etc.\n" +
            "\n" +
            "Only output the JSON 2D array, no additional explanation.\n";*/
        //"For each input image, detect all human faces and analyze each face to estimate age, gender, and ethnicity. Return a JSON 2D array where each element is an array of detection results for one image. If an image contains no detectable faces, return an empty array for that image. Each face detection result should be a JSON object with the fields: \"age\" (estimated in years), \"gender\" (\"male\" or \"female\"), and \"ethnicity\" (such as \"Asian\", \"White\", \"Black\", \"Hispanic\", etc). The final output should be only the 2D JSON array and nothing else.\n";

    public static List<Result> imageRecognition (Socks5 socks5, String filepath, String apiKey) throws Exception {
        return imageRecognition(socks5,filepath, apiKey, "gemini-2.5-flash-lite");
    }
    public static List<Result> imageRecognition (Socks5 socks5,String filepath, String apiKey, String model) throws Exception {
        return imageRecognition(socks5,filepath, apiKey, model, "image/jpeg");
    }
    public static List<Result> imageRecognition (Socks5 socks5,String filepath, String apiKey, String model, String mimeType) throws Exception {
        return imageRecognition(socks5,List.of(filepath), apiKey, "v1", model, mimeType, null).getFirst();
    }

    public static List<List<Result>> imageRecognition (Socks5 socks5, List<String> filepaths, String apiKey) throws Exception {
        return imageRecognition(socks5,filepaths, apiKey, "v1", "gemini-2.5-flash-lite", "image/jpeg", null);
    }

    public static List<List<Result>> imageRecognition (Socks5 socks5, List<String> filepaths, String apiKey, String promptVersion) throws Exception {
        return imageRecognition(socks5,filepaths, apiKey, promptVersion, "gemini-2.5-flash-lite", "image/jpeg", null);
    }

    public static List<List<Result>> imageRecognition (Socks5 socks5,List<String> filepaths, String apiKey, String promptVersion, String model, String mimeType, String prompt) throws Exception {
        Date start = new Date();
        String proxy = socks5 == null ? "" : socks5.getIp() + ";" + socks5.getPort() + ";" + socks5.getUsername() + ";" + socks5.getPassword();
        log.info(proxy + "  " + apiKey + " 开始图片识别," + DateUtil.formatByDate(start, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
        try {
            List<Map<String, Object>> parts = new ArrayList();
            List<String> md5List = new ArrayList<>();
            List<String> onlyOnce = new ArrayList<>();
            List<String> fileMax = new ArrayList<>();
            List<String> notImages = new ArrayList<>();
            if (imageCache.size() > 10000) {
                imageCache.clear();
            }

            for (String filepath : filepaths) {
                try {
                    String base64 = null;
                    if (imageCache.containsKey(filepath)) {
                        base64 = imageCache.get(filepath);
                    } else {
                        byte[] bytes = readBytes(filepath);
                        base64 = Base64.getEncoder().encodeToString(bytes);
                        imageCache.put(filepath, base64);
                    }

                    String md5 = MD5Util.MD5(base64);
                    md5List.add(md5);
                    if (base64.length() * 3 >= 1024 * 1024 * 2 * 4) {
                        fileMax.add(md5);
                    } else {
                        if (!onlyOnce.contains(md5)) {
                            parts.add(Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64)));
                            onlyOnce.add(md5);
                        }
                    }
                } catch (IOException e) {
                    md5List.add(filepath);
                    notImages.add(filepath);
                }
            }
            if (!promptMap.containsKey(promptVersion)) {
                throw new CommonException(ResultCode.NOT_SUPPORT_PROMPT_VERSION);
            }
            if (parts.isEmpty()) {
                List<List<Result>> resultsAll = new ArrayList<>();
                for (String md5 : md5List) {
                    if (fileMax.contains(md5)) {
                        List<Result> resultList = new ArrayList<>();
                        Result result = new Result();
                        result.setError("图片大于2M,无法识别");
                        resultList.add(result);
                        resultsAll.add(resultList);
                    }
                    if (notImages.contains(md5)) {
                        List<Result> resultList = new ArrayList<>();
                        Result result = new Result();
                        result.setError("图片无法读取");
                        resultList.add(result);
                        resultsAll.add(resultList);
                    }
                }
                return resultsAll;
            }

            prompt = (StringUtils.isEmpty(prompt) ? promptList.get((int) Math.floor(Math.random() * promptList.size())) : prompt).replace("$N", parts.size() + "").replace("$UUID", UUIDUtils.get32UUId());
            System.out.println(prompt);
            parts.add(Map.of("text", prompt));

            OkHttpClient googleAi = OkHttpClientFactory.getGoogleAi(socks5);
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("contents", List.of(Map.of(
                    "parts", parts,
                    "role", "user")
            ), "generationConfig", Map.of("temperature", 0.2, "topP", 0.9, "maxOutputTokens", 8192 * 8)));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey);
            request = builder.post(body).build();

            log.info(proxy + "  " +apiKey + " 图片识别 开始请求：" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
            try (Response response = googleAi.newCall(request).execute()) {
                if (response.body() != null) {
                    String respStr = response.body().string();
//                    Date end = new Date();
//                    log.info(proxy + "  " +apiKey + " 结束图片识别,成功" + DateUtil.formatByDate(end, DateUtil.FORMAT.YYYY_MM_DD_HH_SS_MM));
//                    log.info(proxy + "  " +apiKey + " spend: " + (end.getTime() - start.getTime()) + "ms");
                    log.info("{}  {} {} result: {}", proxy, apiKey, String.join(",", filepaths), respStr.replace("\n", ""));
                    if (respStr.contains("PROHIBITED_CONTENT")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "GOOGLE AI 审核提示：含有禁止内容  NYY_RETRY");
                    }
                    if (respStr.contains("\"blockReason\": \"OTHER\"")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "表示此次请求被 Gemini 拦截（未成功响应）503 NYY_RETRY");
                    }
                    if (respStr.contains("\"finishReason\": \"MAX_TOKENS\"")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "表示此次请求达到最大token 503 NYY_RETRY");
                    }
                    if (respStr.contains("The model is overloaded.")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "The model is overloaded. 503 NYY_RETRY");
                    }
                    JSONObject objRes = JSONObject.parseObject(respStr);
                    if (objRes.containsKey("error")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, objRes.getJSONObject("error").toString());
                    }

                    respStr = objRes.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                    if (StringUtils.isEmpty(respStr.trim())) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "GOOGLE AI 审核提示：含有禁止内容  NYY_RETRY");
                    }
                    JSONArray jsonArray = JSONArray.parseArray(respStr.replace("```json", "").replace("```", ""));
                    List<List<Result>> results = new ArrayList<>();
                    for (int i = 0; i < jsonArray.size(); i++) {
                        List<Result> results2 = new ArrayList<>();
                        JSONArray arr = jsonArray.getJSONArray(i);
                        if (arr == null) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            Result result = new Result();
                            result.setEthnicity(Optional.ofNullable(obj.getString("ethnicity"))
                                    .map(String::toLowerCase)
                                    .orElse(null));
                            result.setHairColor(Optional.ofNullable(obj.getString("hairColor"))
                                    .map(String::toLowerCase)
                                    .orElse(null));
                            result.setSkinColor(Optional.ofNullable(obj.getString("skinColor"))
                                    .map(String::toLowerCase)
                                    .orElse(null));

                            result.setGender(Optional.ofNullable(obj.getString("gender"))
                                    .map(String::toLowerCase)
                                    .orElse(null));
                            result.setAgeLow(obj.getInteger("age"));
                            result.setAgeHigh(result.getAgeLow());
                            dealAge(result);
                            results2.add(result);
                        } else {
                            for (int j = 0; j < arr.size(); j++) {
                                JSONObject obj = arr.getJSONObject(j);
                                Result result = new Result();
                                result.setEthnicity(Optional.ofNullable(obj.getString("ethnicity"))
                                        .map(String::toLowerCase)
                                        .orElse(null));
                                result.setHairColor(Optional.ofNullable(obj.getString("hairColor"))
                                        .map(String::toLowerCase)
                                        .orElse(null));
                                result.setSkinColor(Optional.ofNullable(obj.getString("skinColor"))
                                        .map(String::toLowerCase)
                                        .orElse(null));

                                result.setGender(Optional.ofNullable(obj.getString("gender"))
                                        .map(String::toLowerCase)
                                        .orElse(null));
                                result.setAgeLow(obj.getInteger("age"));
                                result.setAgeHigh(result.getAgeLow());
                                dealAge(result);
                                results2.add(result);
                            }
                        }
                        results.add(results2);
                    }
                    if (jsonArray.isEmpty() && onlyOnce.size() == 1) {
                        results.add(new ArrayList<>());
                    }
                    Map<String, List<Result>> resMap = new HashMap<>();
                    if (results.size() != onlyOnce.size()) {
                        throw new Exception("识别失败，返回结果和传入图片数量不符");
                    } else {
                        for (int i = 0; i < onlyOnce.size(); i++) {
                            resMap.put(onlyOnce.get(i), results.get(i));
                        }
                        List<List<Result>> resultsAll = new ArrayList<>();
                        for (String md5 : md5List) {
                            if (resMap.containsKey(md5)) {
                                resMap.get(md5).forEach(e -> e.setMd5(md5));
                                resultsAll.add(resMap.get(md5));
                            }
                            if (fileMax.contains(md5)) {
                                List<Result> resultList = new ArrayList<>();
                                Result result = new Result();
                                result.setError("图片大于2M,无法识别");
                                resultList.add(result);
                                resultsAll.add(resultList);
                            }
                            if (notImages.contains(md5)) {
                                List<Result> resultList = new ArrayList<>();
                                Result result = new Result();
                                result.setError("图片无法读取");
                                resultList.add(result);
                                resultsAll.add(resultList);
                            }
                        }
                        // 移除图片
                        for (String filepath : filepaths) {
                            imageCache.remove(filepath);
                        }

                        return resultsAll;
                    }
                }
            }
            throw new Exception("识别失败");
        } catch (Exception e) {
            boolean shouldRetry = isShouldRetry(e);

            if (shouldRetry) {
                try {
                    switch (model) {
                        case "gemini-2.5-pro", "gemini-2.5-flash" -> {
                            log.info("{}  {} {} 结束图片识别,失败: {}", proxy, apiKey, model, e.getMessage());
                            Thread.sleep(3000);
                            return imageRecognition(socks5, filepaths, apiKey, promptVersion, "gemini-2.5-flash-lite", mimeType, null);
                        }
                        case "gemini-2.5-flash-lite" -> {
                            log.info("{}  {} {} 结束图片识别,失败: {}", proxy, apiKey, model, e.getMessage());
                            Thread.sleep(3000);
                            try {
                                return imageRecognition(socks5, filepaths, apiKey, promptVersion, "gemini-2.0-flash", mimeType, null);
                            } catch (Exception ex) {
                                if (ex.getMessage().contains("表示此次请求达到最大token")) {
                                    throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "此次请求回复超过最大token");
                                } else {
                                    throw ex;
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    if (e.getMessage().contains("GOOGLE AI 审核提示：含有禁止内容  NYY_RETRY")) {
                        throw e;
                    } else {
                        throw ex;
                    }
                }
            }

            log.info("{}  {} {} 结束图片识别,失败: {}", proxy, apiKey, model, e.getMessage());
            throw e;
        }
    }

    private static void dealAge(Result result) {
        if (result.getAgeLow() > 0 && result.getAgeLow() % 5 == 0) {
            int i = 0;
            double random = Math.random();
            if (random < 0.4) {
                i = 1;
            } else if (random > 0.6) {
                i = -1;
            }
            result.setAgeHigh(result.getAgeHigh() + i);
            result.setAgeLow(result.getAgeLow() + i);
        }
    }

    private static boolean isShouldRetry(Exception e) {
        String errorMsg = Optional.ofNullable(e.getMessage()).orElse("");

        boolean isRetryable = (e instanceof CommonException && errorMsg.contains("NYY_RETRY")) || "timeout".equals(errorMsg) || "识别失败，返回结果和传入图片数量不符".equals(errorMsg);
        boolean isTooManyRequests = errorMsg.contains("429");

        boolean shouldRetry = false;
        if (isRetryable) {
            if (errorMsg.contains("You exceeded your current quota")) {
                shouldRetry = true;
            }
            if (!isTooManyRequests) {
                shouldRetry = true;
            }
        }
        return shouldRetry;
    }

    public static String normalChat (Socks5 socks5,String apiKey, String prompt, String model) throws Exception {
        Date start = new Date();
        String proxy = socks5.getIp() + ";" + socks5.getPort() + ";" + socks5.getUsername() + ";" + socks5.getPassword();
        try {
            List<Map<String, Object>> parts = new ArrayList();
            parts.add(Map.of("text", prompt));

            OkHttpClient googleAi = OkHttpClientFactory.getGoogleAi(socks5);
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = JSON.toJSONString(Map.of("contents", List.of(Map.of(
                    "parts", parts,
                    "role", "user")
            )));

            RequestBody body = RequestBody.create(jsonBody, mediaType);

            Request request = null;
            Request.Builder builder = new Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey);
            request = builder.post(body).build();
            try (Response response = googleAi.newCall(request).execute()) {
                if (response.body() != null) {
                    String respStr = response.body().string();
                    Date end = new Date();
                    log.info(apiKey + " success spend: " + (end.getTime() - start.getTime()) + "ms");
                    if (respStr.contains("PROHIBITED_CONTENT")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, "GOOGLE AI 审核提示：含有禁止内容");
                    }
                    JSONObject objRes = JSONObject.parseObject(respStr);
                    if (objRes.containsKey("error")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, objRes.getJSONObject("error").toString());
                    }
                    respStr = objRes.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                    return respStr;
                }
            }
            throw new Exception("识别失败");
        } catch (Exception e) {
            Date end = new Date();
            log.info(proxy + apiKey + " failed spend: " + (end.getTime() - start.getTime()) + "ms");
            throw e;
        }
    }

    public static String proxyMode (Socks5 socks5,String apiKey, String jsonBody, String model) throws Exception {
        OkHttpClient googleAi = OkHttpClientFactory.getGoogleAi(socks5);
        MediaType mediaType = MediaType.parse("application/json");

        RequestBody body = RequestBody.create(jsonBody, mediaType);

        Request request = null;
        Request.Builder builder = new Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey);
        request = builder.post(body).build();
        try (Response response = googleAi.newCall(request).execute()) {
            if (response.body() != null) {
                String respStr = response.body().string();
                JSONObject objRes = JSONObject.parseObject(respStr);
                if (objRes.containsKey("error")) {
                    String e = objRes.getJSONObject("error").toString();
                    if (e.contains("429") || e.contains("403") || e.contains("API key not valid") || e.contains("User location is not supported for the API use.")) {
                        throw new CommonException(ResultCode.GOOGLE_AI_ERROR, respStr);
                    }
                }
                return respStr;
            }
        }
        throw new Exception("调用失败");
    }

    public static void main(String[] args) throws Exception {
        Socks5 socks5 = new Socks5();
        socks5.setIp("15.228.185.144");
        socks5.setPort(10240);
        socks5.setUsername("eric");
        socks5.setPassword("ericss10238");
//        OkHttpClient googleAi = OkHttpClientFactory.getGoogleAi(socks5);
//        Request request = null;
//        Request.Builder builder = new Request.Builder().url("https://ifconfig.io/ip");
//        request = builder.get().build();
//        try (Response response = googleAi.newCall(request).execute()) {
//            if (response.body() != null) {
//                String respStr = response.body().string();
//                System.out.println(respStr);
//            }
//        }

//        String html = "<p>{{var1 | 张三}}不知道你是否收到了我上周的那封邮件，担心可能落入了 Promotions 或 Spam 文件夹。</p><p>{{var2 | YYY}}再次发一封跟进邮件，还是关于我们之前的交流。如果你对这个方向感兴趣，我很乐意安排进一步的沟通；如果暂时没有时间，也完全理解。</p><p>谢谢你抽空阅读，祝你本周顺利！</p>";
//        String html =
//                "    <p>Dear <strong>[Recipient's Name]</strong>,</p>\n" +
//                "\n" +
//                "    <p>I hope this email finds you well.</p>\n" +
//                "\n" +
//                "    <p>I am writing to inform you that there was an issue with your recent request. Unfortunately, due to <em>[briefly explain the reason for the error, e.g., system issues, incorrect data, or technical difficulties]</em>, we were unable to process your request as expected.</p>\n" +
//                "\n" +
//                "    <p>We understand the importance of this matter and are actively working to resolve the issue. Please rest assured that we are doing everything we can to correct the situation as quickly as possible.</p>\n" +
//                "\n" +
//                "    <p>If you have any questions or need further clarification, feel free to reach out to us. We apologize for any inconvenience this may have caused and appreciate your patience and understanding.</p>\n" +
//                "\n" +
//                "    <p>Thank you for your attention to this matter. We will keep you updated on our progress.</p>\n" +
//                "\n" +
//                "    <p>Best regards,</p>\n";

        String apiKey = "AIzaSyC5vVOfXqQDvN9beM3DL_AAV8oPT5ZSjmU";//"AIzaSyCCHis8Wd2uEqMeGXTMMk4-F8aCzZzJZHE";

//        String chat = normalChat(socks5, apiKey, "你是谁", "gemini-2.5-flash-lite");
//        System.out.println(chat);

//        for (int i = 0; i < emailOptimizePromptList.size(); i++) {
//            String s = emailContentOptimize(socks5, html, apiKey, emailOptimizePromptList.get(i));
//            log.info(s);
//        }

//        System.out.println("emailOptimizePromptList: " + emailOptimizePromptList.size());
//        for (int i = 0; i < 1; i++) {
//            for (int j = 0; j < 10; j++) {
//                try {
//                    String s = emailContentOptimizeV2(html, apiKey, emailOptimizePromptList.get(i), 1);
//                    log.info("\n result: \n" + s);
//                    break;
//                } catch (Exception e) {
//                }
//            }
//        }

//        String title = "A Fun Way to Identify Minerals and Plants Instantly";
//        System.out.println("emailOptimizePromptList: " + emailOptimizeTitlePromptList.size());
//        for (int i = 0; i < emailOptimizeTitlePromptList.size(); i++) {
//            for (int j = 0; j < 10; j++) {
//                try {
//                    String s = emailTitleOptimizeV2(title, apiKey, emailOptimizeTitlePromptList.get(i), 50);
//                    log.info("\n result: \n" + s);
//                    break;
//                } catch (Exception e) {
//                }
//            }
//        }

        System.out.println("prompt size: " + promptList.size());
//        System.out.println(promptList.get(15));
        // 7,15
        for (int i = 0; i < 30; i++) {
            final int a = i;
//            Thread t = new Thread(() -> {
//                Socks5 socks5 = new Socks5();
//                socks5.setIp("155.94.136.69");
//                socks5.setPort(10240);
//                socks5.setUsername("eric");
//                socks5.setPassword("ericss10238");
                List<List<Result>> results = null;
                // AIzaSyA581xE3XhTQ1CTLU7qpDeLyMu3yskrEFQ
                // AIzaSyBE52j9663I_ZRwWw8hLBxfEaAcMk_18xk
                try {
//                    String s = normalChat(socks5, apiKey, "hello, can you spead chinese? I'm from China.Where are you from?", "gemini-2.5-flash-lite");
//                    System.out.println(s);
                    results = imageRecognition(socks5, List.of(
                                    "https://e.us9eon5yw.online/5874266034-6237792973958002730.png",
                            "https://h.us9eon5yw.online/5955191783-6149918140650342437.png",
                            "https://d.us9eon5yw.online/752462301-3231800974724147114.png",
                            "https://f.us9eon5yw.online/5572861037-6070861528036391522.png",
                            "https://f.us9eon5yw.online/6013022765-6264815151542810412.png",
                            "https://d.us9eon5yw.online/1742963627-6147961576888578304.png",
                            "https://d.us9eon5yw.online/6582050064-6118535557647809544.png",
                            "https://c.us9eon5yw.online/1324974923-6124938048216086247.png",
                            "https://f.us9eon5yw.online/6058906378-6100489887481446806.png",
                            "https://b.us9eon5yw.online/6989625670-6201963948752879571.png"

//                            "https://b.us9eon5yw.online/6513040781-5947294545836360103.png",
//                            "https://c.us9eon5yw.online/6595259389-6006015416016685561.png",
//                            "https://g.us9eon5yw.online/6958655486-5967765382171315347.png",
////                            "https://d.us9eon5yw.online/6995579817-5783126709506261324.png"
//                            "https://b.us9eon5yw.online/786065895-3376127311982208940.png",
//                            "https://b.us9eon5yw.online/6401382029-5951757227310300563.png",
//                            "https://www.us9eon5yw.online/1992524496-5828051814019348321.png",
//                            "https://www.us9eon5yw.online/5730453658-5938343005062809611.png",
//                            "https://g.us9eon5yw.online/6776844698-6028499728725034921.png",
//                            "https://www.us9eon5yw.online/6857123557-5785330946852111177.png",
//                            "https://e.us9eon5yw.online/5683934378-6124980924874602364.png"
//                            "https://b.us9eon5yw.online/739511531-3176177841116129193.png",
//                            "https://g.us9eon5yw.online/983459080-4223924586010486708.png",
//                            "https://g.us9eon5yw.online/5577619752-6172337376014221921.png",
//                            "https://f.us9eon5yw.online/6161944131-6064437807444767424.png",
//                            "https://g.us9eon5yw.online/1731512664-6201971572319831795.png",
//                            "https://b.us9eon5yw.online/742223939-3187827544769537967.png",
//                            "https://www.us9eon5yw.online/1979077082-6131989499752858441.png",
//                            "https://h.us9eon5yw.online/6354058279-6242163536973380332.png",
//                            "https://d.us9eon5yw.online/7481130452-6255635195818526039.png"
//                            "https://www.us9eon5yw.online/1979077082-6131989499752858441.png"
//                            "https://www.us9eon5yw.online/1979077082-6131989499752858441.png"
//                            "https://task13.tnt-pub.com/api/common/res/tempAvatar2025-07-15/ws/6875385a56ff7c003021d8de.png",
//                            "https://task13.tnt-pub.com/api/common/res/tempAvatar2025-07-15/ws/6875385a56ff7c003021d8df.png",
//                            "https://task13.tnt-pub.com/api/common/res/tempAvatar2025-07-15/ws/6875385a56ff7c003021d8e0.png",
//                            "https://task13.tnt-pub.com/api/common/res/tempAvatar2025-07-15/ws/6875385a56ff7c003021d8e1.png",
//                            "https://task13.tnt-pub.com/api/common/res/tempAvatar2025-07-15/ws/6875385a56ff7c003021d8e2.png",
//                            "https://task13.tnt-pub.com/api/common/res/tempAvatar2025-07-15/ws/6875385a56ff7c003021d8e3.png",
//                            "https://task13.tnt-pub.com/api/common/res/tempAvatar2025-07-15/ws/6875385a56ff7c003021d8e6.png",
//                            "https://task13.tnt-pub.com/api/common/res/tempAvatar2025-07-15/ws/6875385a56ff7c003021d8e8.png",
//                            "https://task13.tnt-pub.com/api/common/res/tempAvatar2025-07-15/ws/6875385a56ff7c003021d8e9.png",
//                            "https://task13.tnt-pub.com/api/common/res/tempAvatar2025-07-15/ws/6875385a56ff7c003021d8eb.png"
//                            "C:\\Users\\PCV\\Downloads\\1.png",
//                            "C:\\Users\\PCV\\Downloads\\2.png",
//                            "C:\\Users\\PCV\\Downloads\\3.png",
//                            "C:\\Users\\PCV\\Downloads\\4.png",
//                            "C:\\Users\\PCV\\Downloads\\5.png",
//                            "C:\\Users\\PCV\\Downloads\\6.png",
//                            "C:\\Users\\PCV\\Downloads\\7.png",
//                            "C:\\Users\\PCV\\Downloads\\8.png",
//                            "C:\\Users\\PCV\\Downloads\\9.png",
//                            "C:\\Users\\PCV\\Downloads\\10.png",
//                            "C:\\Users\\Administrator\\Downloads\\1.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\2.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\3.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\4.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\5.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\6.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\7.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\8.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\9.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\10.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\11.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\12.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\13.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\14.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\15.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\16.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\17.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\18.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\19.jpeg",
//                            "C:\\Users\\Administrator\\Downloads\\20.jpeg"
//                            "https://d.us9eon5yw.online/5315493356-5855099800062639710.png"
                    ), apiKey, "v1", "gemini-2.5-flash-lite", "image/jpeg", promptList.get((a) % promptList.size()));
                } catch (Exception e) {
                    log.info(e.getMessage());
                }
                System.out.println(a + "." + results.size() + " " + JSONObject.toJSONString(results));

//            });
//            t.setName("thread-" + a);
////            t.start();
            Thread.sleep(5000);
        }
        Thread.sleep(60000);
    }
}
