
import gym



from gym.envs.registration import register
register(
    id='FrozenLakeNotSlippery-v0',
    entry_point='gym.envs.toy_text:FrozenLakeEnv',
    kwargs={'map_name' : '4x4', 'is_slippery': False},
    max_episode_steps=100,
    reward_threshold=0.78, # optimum = .8196
)

env = gym.make('FrozenLakeNotSlippery-v0')
obs = env.reset()
print()
env.render()
print("{}".format(obs))

while True:
    obs, rewards, dones, info = env.step(int(input()))
    print("{} {} {} {}".format(obs, rewards, dones, info))
    print()
    env.render()