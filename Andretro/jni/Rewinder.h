#ifndef ANDRETRO_REWINDER_H
#define ANDRETRO_REWINDER_H

#include <vector>
#include <deque>
#include <stdint.h>
#include "Library.h"
#include "Common.h"

class Rewinder
{
    struct Frame
    {
        Frame(uint32_t aLocation, uint32_t aSize) :
            location(aLocation), size(aSize)
        {
        
        }
        
        bool overlaps(const Frame& aOther) const
        {
            const uint32_t myLow = location;
            const uint32_t myHigh = location + size;

            const uint32_t yourLow = aOther.location;
            const uint32_t yourHigh = aOther.location + aOther.size;
            
            return (myLow < yourHigh && yourLow < myHigh);
        }
    
        uint32_t location;
        uint32_t size;
    };

    std::vector<uint8_t> data;
    std::vector<uint8_t> buffer;
    
    uint32_t nextFrame;
    
    std::deque<Frame> frames;

    public:
        Rewinder() :
            data(0), buffer(0), nextFrame(0)
        {
        
        }
    
        void gameLoaded(const Library* aModule)
        {
            buffer.resize(aModule->serialize_size());
            nextFrame = 0;
            frames.clear();
        }
    
        void setSize(int aSize)
        {
            data.resize((aSize >= 0) ? aSize : 0);
            nextFrame = 0;
            frames.clear();
        }
        
        void stashFrame(const Library* aModule)
        {
            if(buffer.size() && data.size())
            {
                if(aModule->serialize(&buffer[0], buffer.size()))
                {
                    nextFrame = ((data.size() - nextFrame) < buffer.size()) ? 0 : nextFrame;
                    memcpy(&data[nextFrame], &buffer[0], buffer.size());
                        
                    // Add to list
                    frames.push_back(Frame(nextFrame, buffer.size()));
                    nextFrame += buffer.size();
                    
                    // Remove any dead entries from the list
                    while((frames.size() > 1) && frames.front().overlaps(frames.back()))
                    {
                        frames.pop_front();
                    }
                }
            }
        }
        
        bool eatFrame(const Library* aModule)
        {
            if(buffer.size() && data.size())
            {
                if(!frames.empty())
                {
                    const Frame loadFrame = frames.back();
                    
                    frames.pop_back();
                    nextFrame = loadFrame.location;
                    return aModule->unserialize(&data[loadFrame.location], loadFrame.size);
                }
                else
                {
                    return false;
                }
            }
            
            return true;
        }
};

#endif
