export const brand = {
  zh: '声析',
  en: 'VOXINSIGHT',
  slogan: '让每一次表达，都有迹可循。',
  about:
    '声析 VOXINSIGHT 是面向大学生和求职者的表达训练原型。当前报告来自预设演示数据，用于观察语速、停顿和流畅度的变化，不判断性格、心理状态或面试结果。',
}

export const user = {
  nickname: '林晓',
  initial: '林',
  school: '明德大学',
  major: '软件工程',
  bio: '正在准备秋招面试，想把自我介绍说得更顺。',
}

export const categories = ['全部', '面试表达', '日常口语', '演讲训练']

export const topics = [
  {
    id: 1,
    category: '面试表达',
    title: '请用 1 分钟做一个自我介绍。',
    difficulty: '入门',
    durationMin: 1,
    durationMax: 2,
    guide: '尽量自然表达，注意语速与停顿。',
    favorite: true,
  },
  {
    id: 2,
    category: '面试表达',
    title: '你最大的优势是什么？',
    difficulty: '入门',
    durationMin: 1,
    durationMax: 2,
    guide: '尽量自然表达，注意语速与停顿。',
    favorite: false,
  },
  {
    id: 3,
    category: '面试表达',
    title: '请介绍一次你解决问题的经历。',
    difficulty: '进阶',
    durationMin: 2,
    durationMax: 3,
    guide: '尽量自然表达，注意语速与停顿。',
    favorite: true,
  },
  {
    id: 4,
    category: '面试表达',
    title: '如果遇到团队意见不一致，你会怎么做？',
    difficulty: '进阶',
    durationMin: 2,
    durationMax: 3,
    guide: '尽量自然表达，注意语速与停顿。',
    favorite: false,
  },
  {
    id: 5,
    category: '演讲训练',
    title: '请围绕一个熟悉的话题进行 2 分钟表达。',
    difficulty: '入门',
    durationMin: 2,
    durationMax: 2,
    guide: '尽量自然表达，注意语速与停顿。',
    favorite: false,
  },
  {
    id: 6,
    category: '日常口语',
    title: '请用日常对话的方式介绍你的一天。',
    difficulty: '入门',
    durationMin: 1,
    durationMax: 2,
    guide: '尽量自然表达，注意语速与停顿。',
    favorite: false,
  },
]

export const reports = {
  1: {
    id: 1,
    topicId: 1,
    title: '请用 1 分钟做一个自我介绍。',
    date: '10月10日',
    shortDate: '10/10',
    duration: '1分08秒',
    score: 72,
    speechRate: 142,
    pauseCount: 8,
    fluency: 76,
    label: '模拟分析结果',
    strengths: ['自我介绍覆盖了身份和求职意向，信息基本完整。'],
    improvements: ['句子中间的长停顿偏多，表达被频繁打断。'],
    suggestions: [
      '可以适当减少句子中间的长停顿，让表达更连贯。',
      '建议按照「背景—行动—结果」组织面试回答。',
    ],
  },
  2: {
    id: 2,
    topicId: 1,
    title: '请用 1 分钟做一个自我介绍。',
    date: '10月12日',
    shortDate: '10/12',
    duration: '1分15秒',
    score: 81,
    speechRate: 168,
    pauseCount: 4,
    fluency: 85,
    label: '模拟分析结果',
    strengths: ['语速更接近自然交谈，停顿从 8 次降到 4 次，表达更连贯。'],
    improvements: ['结果部分仍偏概括，还可以补一个具体例子。'],
    suggestions: [
      '连贯性已有提升，接下来把句中停顿保持在短停顿即可。',
      '回答已有背景和行动，建议在结果处补上具体收获。',
    ],
  },
}

export const historyOrder = [2, 1]

export const trainingStats = {
  count: 2,
  durationSeconds: 143,
  durationText: '2分23秒',
  average: 76.5,
  weeklyDone: 1,
}

export const helpArticles = [
  {
    title: '如何开始一次训练',
    content: '在首页点击「开始训练」进入题库，选择题目后即可进入录音页。当前版本使用预设演示数据，不会调用麦克风。',
  },
  {
    title: '报告里的分数代表什么',
    content:
      '分数、语速、停顿次数和流畅度都是模拟分析结果，只用来对比自己前后两次练习，不能用来判断性格、心理状态或面试能否通过。',
  },
  {
    title: '如何查看上一次练习',
    content: '打开训练历史，按时间倒序查看记录。点进某条记录即可看到那一次自己的报告。',
  },
]

export function durationLabel(topic) {
  if (topic.durationMin === topic.durationMax) return `建议时长 ${topic.durationMin} 分钟`
  return `建议时长 ${topic.durationMin}–${topic.durationMax} 分钟`
}

export function findTopic(id) {
  return topics.find((item) => item.id === Number(id))
}
