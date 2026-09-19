import { complianceDisplay, complianceInfo } from './compliance'

export const PRIVACY_VERSION = '2026-08-07'
export const TERMS_VERSION = '2026-08-07'

export interface LegalSection {
  title: string
  paragraphs: string[]
}

export const privacySections: LegalSection[] = [
  {
    title: '我们处理哪些信息',
    paragraphs: [
      '为建立独立账号并防止答题记录串号，服务端通过微信登录凭证取得 OpenID。OpenID 不在页面或榜单公开展示。',
      '你主动设置的修习名用于个人档案、管理员核对和活动截止后的荣誉榜单。请勿填写手机号、身份证号等敏感信息。',
      '你的答案、判定结果和服务器提交时间用于自动判题、人工复核、个人修习记录及冠军/亚军排序。',
      '你主动提交的反馈类型、情况说明与处理状态用于受理举报、侵权投诉、评分申诉和隐私请求。',
      '首个正式版本不主动获取手机号、微信头像、位置、通讯录、相册或麦克风。',
    ],
  },
  {
    title: '信息如何展示',
    paragraphs: [
      '活动进行中不公开标准答案和最终榜单。活动截止后，榜单向参与者展示修习名、荣誉等级和有效提交时间，不公开原始答案。',
      '管理员可在管理系统中查看修习名、原始答案、判定依据和提交时间，用于答题管理与争议复核。',
    ],
  },
  {
    title: '保存与保护',
    paragraphs: [
      '账号身份、答题记录与排名通过服务端用户 ID 和数据库约束保持关联。提交时间、成绩和排名均由服务端生成，不能由前端修改。',
      `${complianceInfo.accountRetention}。注销后仅按单场保留无法关联到个人的“已注销用户”荣誉等级和排序时间，不保留原始答案。`,
      `${complianceInfo.backupRetention}。`,
    ],
  },
  {
    title: '你的权利',
    paragraphs: [
      '你可以在“我的修习”中查看答题档案、更正修习名，并在“账号与数据”中申请账号注销。',
      `你可以通过“反馈与举报”提交内容投诉、评分申诉或隐私请求。${complianceInfo.reportResponseTime}。`,
      '完整的法定隐私保护指引及运营主体信息，以微信提供的《小程序用户隐私保护指引》页面为准。',
    ],
  },
  {
    title: '运营主体与联系',
    paragraphs: [
      `运营主体：${complianceDisplay(complianceInfo.operatorName)}。`,
      `隐私联系人：${complianceDisplay(complianceInfo.privacyContact)}；联系方式：${complianceDisplay(complianceInfo.privacyEmailOrPhone)}。`,
      `联系地址：${complianceDisplay(complianceInfo.contactAddress)}；数据存储地点：${complianceInfo.storageLocation}。`,
    ],
  },
]

export const termsSections: LegalSection[] = [
  {
    title: '服务内容',
    paragraphs: [
      '问道修习提供中华传统文化与经典哲思主题的限时问答、自动判定、个人记录和截止后荣誉榜单。平台内容用于文化学习与自我回顾，不构成宗教活动、宗教教育、医疗建议、心理诊断或任何专业资质证明。',
    ],
  },
  {
    title: '账号与使用规则',
    paragraphs: [
      '你应使用自己的微信账号进入服务，并为自己的答题操作负责。不得冒用他人身份、攻击系统、批量刷榜或尝试获取未公开的标准答案。',
      '修习名和答案不得包含违法违规、侮辱、广告、联系方式或侵犯他人权益的内容。系统可对用户输入进行必要的内容安全检测。',
    ],
  },
  {
    title: '判题与榜单',
    paragraphs: [
      '系统会忽略答案中的表情符号，其余文字、字数、标点和上下联顺序均按老师预设的两句标准答案精确判定，并记录服务器收到答案的提交时刻。自动结果存在争议时，以管理员保留依据的人工复核结果为准。',
      '榜单是学习激励记录，不代表对个人能力、品格或修习境界的绝对评价。',
    ],
  },
  {
    title: '内容与知识产权',
    paragraphs: [
      '经典原文、现代释义、原创课程和编辑内容的权利归其合法权利人所有。未经许可，不得批量抓取、复制、改编或用于商业传播。',
    ],
  },
  {
    title: '服务变更与联系',
    paragraphs: [
      '因维护、网络或第三方平台原因，服务可能短暂中断。涉及协议或隐私规则的重要变化，会通过版本更新或显著提示重新告知。',
      '如需举报、投诉、评分申诉或提出隐私请求，可通过小程序内“反馈与举报”提交；紧急问题也可使用微信客服。运营主体以微信公众平台展示的信息为准。',
    ],
  },
]
