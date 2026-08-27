<template>
  <div class="home-page">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <h1 class="hero-title">
            一次温暖的对话<br />
            <span class="hl">化孤独为慰藉</span>
          </h1>
          <p class="hero-desc">
            每个深夜,每个焦虑的时刻,我们都在这里。不必独自承受,
            让心与心的连接温暖您的每一天。
          </p>
          <div class="hero-actions">
            <el-button type="primary" class="primary-btn" size="large" @click="goConsultation">
              开始倾诉,获得陪伴
            </el-button>
            <el-button class="secondary-btn" size="large" @click="goEmotional">
              记录心情,释放情感
            </el-button>
          </div>
        </div>

        <div class="hero-visual">
          <div class="robot-ring">
            <img src="@/assets/images/机器人.png" alt="AI助手" class="robot-img" />
          </div>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="features">
      <div class="features-inner">
        <div
          v-for="f in features"
          :key="f.title"
          class="feature-card"
        >
          <div class="feature-icon" :style="{ background: f.bg }">
            <el-icon :size="24" :color="f.color">
              <component :is="f.icon" />
            </el-icon>
          </div>
          <h3 class="feature-title">{{ f.title }}</h3>
          <p class="feature-desc">{{ f.desc }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import {
  ChatLineRound,
  Sunny,
  Reading,
  Bell
} from '@element-plus/icons-vue'

const router = useRouter()

function goConsultation() {
  router.push('/consultation')
}
function goEmotional() {
  router.push('/emotional')
}

const features = [
  {
    icon: ChatLineRound,
    title: 'AI心理咨询',
    desc: '24小时全天候AI心理助手,随时陪您聊心,专业而温暖的对话帮您理清情绪。',
    bg: 'linear-gradient(135deg,#d6f3e4,#a5e6c4)',
    color: '#16a34a'
  },
  {
    icon: Sunny,
    title: '情绪追踪',
    desc: '每天记录心情,可视化展示情绪曲线,让您看到自己的变化,更加了解自己。',
    bg: 'linear-gradient(135deg,#ffe4b3,#ffc58a)',
    color: '#f97316'
  },
  {
    icon: Reading,
    title: '心理知识库',
    desc: '精选心理健康科普文章,从情绪管理到人际沟通,为您带来科学的调节方法。',
    bg: 'linear-gradient(135deg,#ddebff,#b3cfff)',
    color: '#3b82f6'
  },
  {
    icon: Bell,
    title: '私密且安全',
    desc: '对话记录只属于您,严格的权限保护,让您在安全的环境下敞开心扉。',
    bg: 'linear-gradient(135deg,#ffe0e6,#ffbfcc)',
    color: '#ef4444'
  }
]
</script>

<style lang="scss" scoped>
.home-page {
  width: 100%;

  .hero {
    background: linear-gradient(135deg, #3ea6a1 0%, #5eb8a8 60%, #84c9b5 100%);
    min-height: 520px;

    .hero-inner {
      max-width: 1200px;
      margin: 0 auto;
      padding: 72px 32px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 24px;
    }

    .hero-text {
      flex: 1;
      color: #fff;
      max-width: 640px;

      .hero-title {
        font-size: 44px;
        line-height: 1.25;
        margin: 0 0 18px;
        font-weight: 800;

        .hl {
          color: #ffe08a;
        }
      }

      .hero-desc {
        font-size: 16px;
        line-height: 1.8;
        margin: 0 0 28px;
        color: rgba(255, 255, 255, 0.92);
        max-width: 540px;
      }

      .hero-actions {
        display: flex;
        gap: 14px;
        flex-wrap: wrap;

        .primary-btn {
          background: #ffffff;
          color: #2d7d7f;
          border: none;
          font-weight: 600;
          padding: 14px 26px;
          border-radius: 10px;
          box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);

          &:hover {
            background: #fff4d8;
            color: #2d7d7f;
          }
        }

        .secondary-btn {
          background: transparent;
          color: #ffffff;
          border: 1.5px solid rgba(255, 255, 255, 0.8);
          font-weight: 600;
          padding: 14px 26px;
          border-radius: 10px;

          &:hover {
            background: rgba(255, 255, 255, 0.12);
            color: #ffffff;
          }
        }
      }
    }

    .hero-visual {
      width: 340px;
      max-width: 40%;
      display: flex;
      justify-content: center;

      .robot-ring {
        width: 240px;
        height: 240px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.18);
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 0 0 16px rgba(255, 255, 255, 0.1);

        .robot-img {
          width: 130px;
          height: 130px;
          border-radius: 50%;
          background: #ffffff;
          padding: 18px;
          box-sizing: border-box;
          box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12);
        }
      }
    }
  }

  .features {
    background: #ffffff;
    padding: 48px 0 64px;

    .features-inner {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 32px;
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 22px;
    }

    .feature-card {
      padding: 24px;
      border-radius: 14px;
      border: 1px solid #eef2f6;
      background: #fff;
      transition: transform 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        transform: translateY(-3px);
        box-shadow: 0 10px 22px rgba(0, 0, 0, 0.06);
      }

      .feature-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 14px;
      }

      .feature-title {
        font-size: 17px;
        margin: 0 0 8px;
        color: #1f2937;
      }

      .feature-desc {
        margin: 0;
        color: #6b7280;
        font-size: 14px;
        line-height: 1.7;
      }
    }
  }
}

@media (max-width: 820px) {
  .home-page {
    .hero .hero-inner {
      flex-direction: column-reverse;
      padding: 40px 20px;
      text-align: center;

      .hero-text {
        .hero-title {
          font-size: 32px;
        }
        .hero-actions {
          justify-content: center;
        }
      }
      .hero-visual {
        max-width: 100%;
      }
    }
  }
}
</style>
